(ns kotori.procedure.dmm.api
  (:require
   [clojure.string :as str]
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.product :as product]
   [kotori.lib.firestore :as fs]
   [kotori.lib.json :as json]
   [kotori.lib.provider.dmm :as client]
   [kotori.lib.time :as time]))

(def campaigns-path "providers/dmm/campaigns")

(defn make-campaign-products-path [id]
  (str campaigns-path "/" id "/" "products"))

(defn- ->items [resp]
  (-> resp
      :result
      :items))

(defn get-product [{:keys [cid env]}]
  (let [{:keys [api-id affiliate-id]}
        env
        creds (client/->Credentials api-id affiliate-id)
        resp  (client/search-product creds {:cid cid})]
    (-> resp
        (->items)
        (first))))

(defn get-products-by-cids
  "APIの並列実行をする.呼び出し回数制限もあるためリストのサイズに注意"
  [{:keys [cids env]}]
  (let [products (->> cids
                      (map (fn [cid] {:env env :cid cid}))
                      (pmap get-product)
                      (doall))]
    (into [] products)))

(defn get-products
  "1回のget requestで最大100つの情報が取得できる.
  それ以上取得する場合はoffsetによる制御が必要."
  [{:keys [env offset hits keyword article article-id]
    :or   {offset 1 hits 100}}]
  {:pre [(<= hits 100)]}
  (let [{:keys [api-id affiliate-id]}
        env
        creds (client/->Credentials api-id affiliate-id)
        req   (cond->
               {:offset offset :sort "rank" :hits hits}
                keyword    (assoc :keyword keyword)
                article    (assoc :article article)
                article-id (assoc :article_id article-id))
        resp  (client/search-product creds req)
        items (->items resp)]
    items))

(defn get-products-bulk
  [{:keys [env hits]}]
  (let [page            (quot hits 100)
        mod-hits        (mod hits 100)
        xf              (comp (map #(* % 100))
                              (map #(+ % 1))
                              (map (fn [offset]
                                     {:env env :offset offset :hits 100})))
        req-params-base (into [] xf (range page)) ;; transducer
        last-offset     (+ (* page 100) 1)
        req-params      (if (zero? mod-hits)
                          req-params-base
                          (conj req-params-base
                                {:env env :offset last-offset :hits mod-hits}))
        products        (->> req-params
                             (pmap get-products)
                             (doall))]
    (reduce concat products)))

(defn get-campaign-products
  "キャンペーンの動画一覧の取得は
  keywordにキャンペーン名を指定することで取得可能.
  キャンペーン対象商品は500に届かないことが多いようなので
  とりあえずhitsのdefaultを500に設定しておく."
  [{:keys [env title hits] :or {hits 500}}]
  {:pre [(<= hits 500)]}
  (get-products-bulk {:env env :keyword title :hits hits}))

(defn crawl-product! "
  1. 指定されたcidのcontent情報を取得.
  2. Firestoreへ 情報を保存."
  [{:keys [db cid] :as m}]
  (let [product (get-product m)
        ts      (time/fs-now)
        data    (-> product
                    product/->data)
        path    (fs/doc-path product/coll-path cid)]
    (fs/set! db path data)
    (fs/set! db path {:last-crawled-time ts})
    data))

;; ランキングとdmm collへのtimestamp書き込みはしない.
(defn crawl-products-by-cids!
  [{:keys [db] :as params}]
  (let [products   (get-products-by-cids params)
        count      (count products)
        ts         (time/fs-now)
        xf         (comp (map product/->data)
                         (map #(product/set-crawled-timestamp ts %))
                         (map json/->json))
        docs       (transduce xf conj products)
        batch-docs (fs/make-batch-docs
                    "cid" product/coll-path docs)]
    (fs/batch-set! db batch-docs)
    {:count     count
     :timestamp ts
     :products  docs}))

;; TODO 500以上の書き込み対応.
;; firestroreのbatch writeの仕様で一回の書き込みは500まで.
;; そのため500単位でchunkごとに書き込む.
;; また Fieldに対するincや配列への追加も1つの書き込みとなる.
(defn crawl-products!
  [{:keys [db] :as params}]
  (let [products   (get-products-bulk params)
        count      (count products)
        ts         (time/fs-now)
        xf         (comp (map product/->data)
                         (map #(product/set-crawled-timestamp ts %))
                         (map-indexed product/set-rank-popular)
                         (map json/->json))
        docs       (transduce xf conj products)
        batch-docs (fs/make-batch-docs
                    "cid" product/coll-path docs)]
    (fs/batch-set! db batch-docs)
    (fs/set! db dmm/doc-path {:products-crawled-time ts})
    {:count     count
     :timestamp ts
     :products  docs}))

;; TODO
;; (defn assoc-tweet->product!
;;   [{:keys [db] :as params}]
;;   nil)

;; 引数はDMM APIで取得できた :campaignのkeyに紐づくMapをそのまま利用.
;; {:date_begin \"2022-03-28 10:00:00\",
;;  :date_end   \"2022-03-30 10:09:59\",
;;  :title      \"新生活応援30％OFF第6弾\"}
(defn campaign->id
  [{:keys [date_begin date_end title]}]
  (let [begin (first (str/split date_begin #" "))
        end   (first (str/split date_end #" "))]
    ;; 文字列ソートのためにbegin_endをprefixsとする.
    (str begin "_" end "_" title)))

(defn- product->campaign [product]
  (-> product
      (:campaign)
      (first)))

(defn crawl-campaign-products!
  "キャンペーン動画情報をFirestoreに保存する.
  保存の際のキャンペーンIDはキャンペーン期間とタイトルから独自に生成する.
  キャンペーンタイトルのみをIDにすると定期開催されるものに対応できない."
  [{:keys [db] :as params}]
  (let [products               (get-campaign-products params)
        count                  (count products)
        id                     (-> products
                                   (first)
                                   (product->campaign)
                                   (campaign->id))
        campaign-products-path (make-campaign-products-path id)
        ts                     (time/fs-now)
        batch-docs             (->>
                                products
                                (map product/->data)
                                (map #(product/set-crawled-timestamp ts %))
                                (fs/make-batch-docs
                                 "cid" campaign-products-path))]
    (fs/batch-set! db batch-docs)
    {:count count}))

(defn prepare-campaign!
  [{:keys [db env title]}]
  (let [product   (first (get-campaign-products
                          {:hits 1 :title title :env env}))
        campaign  (product->campaign product)
        id        (campaign->id campaign)
        coll-path (make-campaign-products-path id)
        data      (product/->data product)
        cid       (get data "cid")]
    (doto db
      (fs/set! (fs/doc-path coll-path cid) data)
      (fs/set! (fs/doc-path campaigns-path id) campaign))
    campaign))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[devtools :refer [env]])
  (require '[firebase :refer [db]])
  (require '[firestore-clj.core :as f])

  (def product (get-product {:cid "ssis00337" :env (env)}))
  (def products (get-product-bulk {:cids ["ssis00337" "hnd00967"]
                                   :env  (env)}))

  (def products (get-products {:env (env) :hits 10}))
  (def products (get-products-bulk {:env (env) :hits 450}))
  (count products)

  (def product (crawl-product! {:db (db) :env (env) :cid "hnd00967"}))
  (def products (crawl-products! {:db (db) :env (env) :hits 10}))

  (def products (crawl-campaign-products!
                 {:db    (db) :env (env)
                  :title "新生活応援30％OFF第6弾"}))

  (fs/set! (db) dmm-doc-path {:products-crawled-time
                              (time/fs-now)})
  )

(comment

  (->> (range 4)
       (map #(+ (* % 100) 1))
       (map (fn [offset]
              {:offset offset :hits 100}))
       (into []))

  (def xform (comp (map #(+ (* % 100) 1))
                   (map (fn [offset]
                          {:offset offset :hits 100}))))
  (transduce xform conj (range 4))
  (into [] xform (range 4))
  )

(comment
  (campaign->id {:date_begin "2022-03-28 10:00:00",
                 :date_end   "2022-03-30 10:09:59",
                 :title      "新生活応援30％OFF第6弾"})

  (def campaign (prepare-campaign!
                 {:db    (db) :env (env)
                  :title "新生活応援30％OFF第6弾"}))
  )
