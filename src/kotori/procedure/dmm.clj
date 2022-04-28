(ns kotori.procedure.dmm
  (:require
   [clojure.string :as str]
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.product :as product]
   [kotori.domain.kotori :refer [guest-user]]
   [kotori.domain.tweet.qvt :as qvt]
   [kotori.lib.firestore :as fs]
   [kotori.lib.json :as json]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.public :as public]
   [kotori.lib.time :as time]
   [kotori.procedure.strategy.dmm :as st]))

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
        creds (api/->Credentials api-id affiliate-id)
        resp  (api/search-product creds {:cid cid})]
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
        creds (api/->Credentials api-id affiliate-id)
        req   (cond->
               {:offset offset :sort "rank" :hits hits}
                keyword    (assoc :keyword keyword)
                article    (assoc :article article)
                article-id (assoc :article_id article-id))
        resp  (api/search-product creds req)
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

(defn get-page [{:keys [cid]}]
  (public/get-page cid))

(defn scrape-page!
  [{:keys [db cid] :as m}]
  (let [page (get-page m)
        ts   (time/fs-now)
        data (product/page->data page)
        path (fs/doc-path product/coll-path cid)]
    (fs/set! db path data)
    (fs/set! db path {:last-scraped-time ts})
    data))

(defn get-page-bulk
  "パラレルでスクレイピングをかけるため一瞬で終わる, すごい."
  [{:keys [cids]}]
  (->> cids
       (map (fn [cid] {:cid cid}))
       (pmap get-page)
       (into [])))

(defn scrape-pages!
  [{:keys [cids db]}]
  (let [pages (get-page-bulk {:cids cids})
        ts    (time/fs-now)]
    (->> pages
         (map #(product/set-scraped-timestamp ts %))
         (map #(json/->json %))
         (fs/make-batch-docs "cid" product/coll-path)
         (fs/batch-set! db))
    (fs/set! db dmm/doc-path {:products-scraped-time ts})
    ts))

(defn get-qvts-without-desc [{:keys [db screen-name limit]}]
  (let [products (st/select-tweeted-products
                  {:db db :screen-name screen-name :limit limit})]
    (->> products
         (remove #(contains? % :description))
         (map qvt/doc->)
         (into []))))

(defn crawl-qvt-descs! [{:keys [db limit] :or {limit 300}}]
  (let [params {:db          db
                :screen-name guest-user
                :limit       limit}
        cids   (->> (get-qvts-without-desc params)
                    (map :cid))]
    (scrape-pages! {:db db :cids cids})))

(defn crawl-product! "
  1. 指定されたcidのcontent情報を取得.
  2. Firestoreへ 情報を保存."
  [{:keys [db cid] :as m}]
  (let [product (get-product m)
        ts      (time/fs-now)
        data    (-> product
                    product/api->data)
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
        xf         (comp (map product/api->data)
                         (map #(product/set-crawled-timestamp ts %))
                         (map json/->json))
        docs       (transduce xf conj products)
        batch-docs (fs/make-batch-docs
                    "cid" product/coll-path docs)]
    (fs/batch-set! db batch-docs)
    {:count     count
     :timestamp ts
     :products  docs}))

(defn- get-target-desc-cids [db]
  (let [last-crawled-time
        (fs/get-in db dmm/doc-path "products_crawled_time")
        query (fs/query-filter "last_crawled_time" last-crawled-time)]
    (->> (fs/get-docs db product/coll-path query)
         (remove #(:description %))
         (map :cid)
         (into []))))

;; TODO 500以上の書き込み対応.
;; firestroreのbatch writeの仕様で一回の書き込みは500まで.
;; そのため500単位でchunkごとに書き込む.
;; また Fieldに対するincや配列への追加も1つの書き込みとなる.
(defn crawl-products!
  [{:keys [db] :as params}]
  (let [products   (get-products-bulk params)
        count      (count products)
        ts         (time/fs-now)
        xf         (comp (map product/api->data)
                         (map #(product/set-crawled-timestamp ts %))
                         (map-indexed product/set-rank-popular)
                         (map json/->json))
        docs       (transduce xf conj products)
        batch-docs (fs/make-batch-docs
                    "cid" product/coll-path docs)]
    (fs/batch-set! db batch-docs)
    (fs/set! db dmm/doc-path {:products-crawled-time ts})
    ;; descritionの追加スクレイピング. 取得済みのものはスキップ.
    ;; この関数は定期実行を想定しているので
    ;; 強制的に更新したいときは手動で関数をたたいて更新する.
    (when-let [cids (get-target-desc-cids db)]
      (scrape-pages! {:db db :cids cids}))
    {:count     count
     :timestamp ts
     :products  docs}))

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
                                (map product/api->data)
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
        data      (product/api->data product)
        cid       (get data "cid")]
    (doto db
      (fs/set! (fs/doc-path coll-path cid) data)
      (fs/set! (fs/doc-path campaigns-path id) campaign))
    campaign))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[firebase :refer [db-prod db-dev db]]
           '[devtools :refer [->screen-name env]])
  )

(comment
  (def product (get-product {:cid "ssis00337" :env (env)}))

  (def products (get-products {:env (env) :hits 10}))
  (def products (get-products-bulk {:env (env) :hits 450}))
  (count products)

  (def product (crawl-product! {:db (db) :env (env) :cid "hnd00967"}))
  (def products (crawl-products! {:db (db) :env (env) :hits 150}))

  ;; 1秒以内に終わる
  (def page (get-page {:cid "pred00294"}))
  (def resp (scrape-page! {:cid "ebod00874" :db (db)}))

  (def cids (->> (get-products {:env (env) :hits 100})
                 (map :content_id)
                 (into [])))
  ;; 並列実行で1秒で完了.
  (def resp (get-page-bulk {:cids cids :db (db)}))
  (def resp (scrape-pages! {:cids cids :db (db)}))


  (def resp (get-target-desc-cids (db)))

  (def products (crawl-campaign-products!
                 {:db    (db) :env (env)
                  :title "新生活応援30％OFF第6弾"}))
  )

(comment
  (def resp (get-qvts-without-desc {:db          (db-dev)
                                    :screen-name guest-user
                                    :limit       50}))
  (def resp (crawl-qvt-descs! {:db (db-dev) :limit 50}))

  )

(comment
  (campaign->id {:date_begin "2022-03-28 10:00:00",
                 :date_end   "2022-03-30 10:09:59",
                 :title      "新生活応援30％OFF第6弾"})

  (def campaign (prepare-campaign!
                 {:db    (db) :env (env)
                  :title "新生活応援30％OFF第6弾"}))
  )
