(ns kotori.procedure.dmm.product
  (:require
   [clojure.string :as str]
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.product :as product]
   [kotori.domain.tweet.qvt :as qvt]
   [kotori.lib.firestore :as fs]
   [kotori.lib.json :as json]
   [kotori.lib.provider.dmm.product :as lib]
   [kotori.lib.provider.dmm.public :as public]
   [kotori.lib.time :as time]
   [kotori.procedure.strategy.dmm :as st]))

(def campaigns-path "providers/dmm/campaigns")

(defn make-campaign-products-path [id]
  (str campaigns-path "/" id "/" "products"))

(defn get-campaign-products
  "キャンペーンの動画一覧の取得は
  keywordにキャンペーン名を指定することで取得可能.
  キャンペーン対象商品は500に届かないことが多いようなので
  とりあえず limitのdefaultを500に設定しておく."
  [{:keys [limit] :or {limit 500} :as m}]
  {:pre [(<= limit 500)]}
  (lib/get-products m))

(defn scrape-page
  [{:keys [db cid]}]
  (let [page (public/get-page cid)
        ts   (time/fs-now)
        data (product/page->data page)
        path (fs/doc-path product/coll-path cid)]
    (fs/set! db path data)
    (fs/set! db path {:last-scraped-time ts})
    data))

(defn get-page-bulk
  [{:keys [cids]}]
  (->> cids
       (lib/request-bulk public/get-page)
       (into [])))

(defn scrape-pages!
  [{:keys [cids db coll-path ts] :or {ts        (time/fs-now)
                                      coll-path product/coll-path}}]
  (when-let [pages (get-page-bulk {:cids cids})]
    (->> pages
         (map #(product/set-scraped-timestamp ts %))
         (map #(json/->json %))
         (fs/make-batch-docs "cid" coll-path)
         (fs/batch-set! db))
    {:timestamp ts
     :count     (count pages)
     :pages     pages}))

(defn get-qvts-without-keyword [{:as m} keyword]
  (let [products (st/select-tweeted-products m)]
    (->> products
         (remove #(contains? % keyword))
         (map qvt/doc->)
         (into []))))

(defn get-qvts-without-summary [{:as m}]
  (get-qvts-without-keyword m :summary))

(defn get-qvts-without-desc [{:as m}]
  (get-qvts-without-keyword m :description))

(defn crawl-qvt-descs! [{:as m :or {limit 300} :keys [db]}]
  (let [cids (->> (get-qvts-without-desc m)
                  (map :cid))]
    (when (> (count cids) 0)
      (scrape-pages! {:db db :cids cids}))))

(defn save-product! [db coll-path product ts]
  (let [data (-> product product/api->data)
        cid  (:cid data)
        path (fs/doc-path coll-path cid)]
    (doto db
      (fs/set! path data)
      (fs/set! path {:last-crawled-time ts}))
    data))

(defn crawl-product! "
  1. 指定されたcidのcontent情報を取得.
  2. Firestoreへ 情報を保存."
  ([m]
   (crawl-product! m product/coll-path))
  ([{:keys [db cid] :as m} coll-path]
   {:pre [(string? cid)]}
   (let [ts      (time/fs-now)
         product (lib/get-video m)]
     (save-product! db coll-path product ts))))

;; ランキングとdmm collへのtimestamp書き込みはしない.
(defn crawl-products-by-cids!
  [{:keys [db] :as params}]
  (let [products   (lib/get-products-by-cids params)
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

(defn- get-target-desc-cids [db coll-path timestamp-key]
  (let [last-crawled-time
        (fs/get-in db dmm/doc-path timestamp-key)
        query (fs/query-filter "last_crawled_time" last-crawled-time)]
    (->> (fs/get-docs db coll-path query)
         (remove #(:description %))
         (map :cid)
         (into []))))

(defn save-products! [db coll-path products ts]
  (let [xf         (comp (map product/api->data)
                         (map #(product/set-crawled-timestamp ts %))
                         (map-indexed product/set-rank-popular)
                         (map json/->json))
        docs       (transduce xf conj products)
        batch-docs (fs/make-batch-docs "cid" coll-path docs)]
    (fs/batch-set! db batch-docs)
    docs))

(defn update-crawled-time! [db field value]
  (fs/assoc! db dmm/doc-path field value))

;; descritionの追加スクレイピング. 取得済みのものはスキップ.
;; この関数は定期実行を想定しているので
;; 強制的に更新したいときは手動で関数をたたいて更新する.
(defn scrape-desc-if! [db coll-path timestamp-key]
  (let [cids (get-target-desc-cids db coll-path timestamp-key)]
    (when (and cids (< 0 (count cids)))
      (scrape-pages! {:db        db
                      :cids      cids
                      :coll-path coll-path}))))

;; FIXME 500以上の書き込み対応.
;; firestroreのbatch writeの仕様で一回の書き込みは500まで.
;; そのため500単位でchunkごとに書き込む.
(defn crawl-products!
  [{:keys [db] :as params}]
  (let [timestamp-key "products_crawled_time"
        ts            (time/fs-now)
        coll-path     product/coll-path]
    (when-let [products (lib/get-products params)]
      (doto db
        (save-products! coll-path products ts)
        (update-crawled-time! timestamp-key ts)
        (scrape-desc-if! coll-path timestamp-key))
      ;; ここでproductsオブジェクトを戻すとGCRでエラーした.
      ;; 詳細は未調査だけどMapを返せば正常終了.
      {:timestamp ts
       :count     (count products)
       :products  products})))

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
                          {:limit 1 :title title :env env}))
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
           '[tools.dmm :refer [creds]]
           '[devtools :refer [->screen-name env]])
  )

(comment
  (def product (crawl-product! {:db    @db
                                :creds @creds
                                :cid   "hnd00967"}))
  (def products (crawl-products! {:db    @db-dev
                                  :creds @creds
                                  :limit 300}))

  ;; 1秒以内に終わる
  (def page (public/get-page  "pred00294"))
  (def resp (scrape-page {:cid "ebod00874" :db (db)}))

  (def cids (->> (lib/get-products {:creds @creds
                                    :limit 10})
                 (map :content_id)
                 (into [])))

  ;; 並列実行
  (def resp (get-page-bulk {:cids cids :db (db)}))
  (def resp (scrape-pages! {:cids cids :db (db)}))

  (def products (crawl-campaign-products!
                 {:db    (db) :env (env)
                  :title "新生活応援30％OFF第6弾"}))
  )

(comment
  (def resp (get-qvts-without-desc {:db    (db-prod)
                                    :limit 300}))

  (def resp (crawl-qvt-descs! {:db    (db-prod)
                               :limit 100}))

  )

(comment
  (campaign->id {:date_begin "2022-03-28 10:00:00",
                 :date_end   "2022-03-30 10:09:59",
                 :title      "新生活応援30％OFF第6弾"})

  (def campaign (prepare-campaign!
                 {:db    (db) :env (env)
                  :title "新生活応援30％OFF第6弾"}))
  )
