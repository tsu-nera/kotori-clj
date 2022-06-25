(ns kotori.procedure.dmm.doujin
  (:require
   [kotori.domain.dmm.genre.doujin :as genre]
   [kotori.domain.dmm.product
    :as d
    :refer [doujin-coll-path girls-coll-path]]
   [kotori.domain.kotori.core :refer [kotori->af-id]]
   [kotori.lib.firestore :as fs]
   [kotori.lib.json :as json]
   [kotori.lib.kotori :refer [ng->ok next->swap-af-id]]
   [kotori.lib.provider.dmm.core :refer [swap-af-id]]
   [kotori.lib.provider.dmm.doujin :as lib]
   [kotori.lib.time :as time]
   [kotori.procedure.dmm.product :as product]
   [kotori.procedure.strategy.dmm :as st]))

(defn crawl-product! [{:keys [db] :as m}]
  (when-let [product (lib/get-product m)]
    (product/save-product! db
                           doujin-coll-path
                           product
                           (time/fs-now))))

(defn crawl-products!
  "男性向け"
  [{:keys [db] :as m}]
  (let [ts   (time/fs-now)
        docs (->> (lib/get-boys-products m)
                  (map lib/api->data)
                  (map (fn [m] (d/set-crawled-timestamp ts m)))
                  (map json/->json))]
    (doto db
      (product/save-products! doujin-coll-path docs)
      (product/update-crawled-time! ts "doujin" genre/for-boy-id))
    {:timestamp ts
     :count     (count docs)
     :products  docs}))

(defn- get-target-cids [db coll-path products]
  (let [cids (map :content_id products)]
    (->> (fs/get-docs-by-ids db coll-path cids)
         (remove #(:section %))
         (map :cid)
         (into []))))

(defn scrape-pages!
  [{:keys [cids db coll-path ts]
    :or   {ts (time/fs-now)}}]
  (when-let [pages (keep identity
                         (lib/get-page-bulk cids))]
    (let [sections (map lib/raw->section pages)]
      (->> (map vector cids sections)
           (map (fn [[cid section]] {:cid cid :section section}))
           (map #(d/set-scraped-timestamp ts %))
           (map #(json/->json %))
           (fs/make-batch-docs "cid" coll-path)
           (fs/batch-set! db)))
    {:timestamp ts
     :count     (count pages)
     :pages     pages}))

(defn scrape-section-if! [db products]
  (let [cids (get-target-cids db girls-coll-path products)]
    (when (and cids (< 0 (count cids)))
      (scrape-pages! {:db        db
                      :cids      cids
                      :coll-path girls-coll-path}))))

(defn crawl-girls-products!
  "女性向け"
  [{:keys [db] :as m}]
  (let [ts       (time/fs-now)
        products (lib/get-girls-products m)
        docs     (->> products
                      (map lib/api->data)
                      (map (fn [m] (d/set-crawled-timestamp ts m)))
                      (map json/->json))]
    (doto db
      (product/save-products! girls-coll-path docs)
      (product/update-crawled-time! ts "doujin" genre/for-girl-id)
      (scrape-section-if! products))
    {:timestamp ts
     :count     (count docs)
     :products  docs}))

(defn crawl-voice-products! [{:keys [db] :as m}]
  (let [ts   (time/fs-now)
        docs (->> (lib/get-voice-products m)
                  (map lib/api->data)
                  (map (fn [m] (d/set-crawled-timestamp ts m)))
                  (map json/->json))]
    (doto db
      (product/save-products! doujin-coll-path docs)
      (product/update-crawled-time! ts "doujin" nil))
    {:timestamp ts
     :count     (count docs)
     :products  docs}))

(defn image-product? [p]
  (or (= "cg" (:format p))
      (= "comic" (:format p))))

(defn voice-product? [p]
  (= "voice" (:format p)))

(defn tl-product? [p]
  (= "tl" (:section p)))

(defn bl-product? [p]
  (= "bl" (:section p)))

(defmulti make-strategy :code)

(defmethod make-strategy "0002" [_]
  [(filter voice-product?)
   (st/->st-include genre/chikubi-ids)])

(defmethod make-strategy "0003" [_]
  [(filter image-product?)
   (filter bl-product?)])

(defmethod make-strategy "0026" [_]
  [(filter image-product?)
   (filter tl-product?)])

(defmethod make-strategy "0029" [_]
  [(filter image-product?)])

(defmethod make-strategy "0031" [_]
  [(filter voice-product?)
   (st/->st-exclude genre/chikubi-ids)])

(defmethod make-strategy "0034" [_]
  [(filter image-product?)
   (filter bl-product?)])

(defmethod make-strategy :default [_]
  [])

;; TODO とりあえずやっつけで分岐するがあとでインタフェースで解決する.
(defn select-scheduled-image
  [{:keys [info db limit creds coll-path]
    :as   m
    :or   {limit 200}}]
  (let [genre-id (:genre-id info)
        products (lib/get-products {:genre-id genre-id
                                    :creds    creds
                                    :limit    limit})
        xst      (make-strategy info)
        doc-ids  (map :content_id products)]
    (->> (st/select-scheduled-products-with-xst
          m xst coll-path doc-ids)
         (take limit))))

(defn select-next-image [{:keys [info] :as params}]
  (let [doc        (first (select-scheduled-image params))
        cid        (:cid doc)
        title      (-> (:title doc) ng->ok)
        af-id      (kotori->af-id info)
        image-urls (lib/get-image-urls cid)]
    {:cid           cid
     :title         title
     :affiliate-url (swap-af-id af-id (:affiliate-url doc))
     :urls          image-urls}))

(defn select-scheduled-voice
  [{:keys [info db limit creds]
    :as   m
    :or   {limit 100}}]
  (let [products (lib/get-voice-products {:creds creds
                                          :limit limit})
        xst      (make-strategy info)
        doc-ids  (map :content_id products)]
    (->> (st/select-scheduled-products-with-xst
          m xst doujin-coll-path doc-ids)
         (take limit))))

(defn- select-while-url-exists [docs]
  (let [doc  (first docs)
        cid  (:cid doc)
        urls (lib/get-voice-urls cid)]
    (if (> (count urls) 0)
      {:cid           cid
       :urls          urls
       :affiliate-url (:affiliate-url doc)
       :title         (-> (:title doc) ng->ok)}
      (recur (rest docs)))))

(defn select-next-voice [{:keys [info] :as params}]
  (let [docs  (select-scheduled-voice params)
        af-id (kotori->af-id info)]
    (-> (select-while-url-exists (take 5 docs))
        (next->swap-af-id af-id))))

(comment
  (require
   '[devtools :refer [code->kotori]]
   '[tools.dmm :refer [creds]]
   '[firebase :refer [db db-prod db-dev]])

  (def cid "d_227233")
  (def resp (lib/get-product {:cid cid :creds (creds)}))
  (def resp (lib/get-products {:creds (creds) :hits 100}))

  (def urls (map #(get-in % [:imageURL :list]) resp))

  (def resp (crawl-product! {:db (db) :cid cid :creds (creds)}))
  (def products (crawl-products! {:db    (db-prod)
                                  :creds (creds)
                                  :limit 300}))

  (def girls (:products
              (crawl-girls-products! {:db    (db-prod)
                                      :creds (creds)
                                      :limit 300})))
  (count girls)

  (def products (crawl-voice-products! {:db    (db)
                                        :creds (creds)
                                        :limit 100}))

  (def kotori (code->kotori "0034"))
  (def products
    (select-scheduled-image
     {:db        (db-dev)
      :info      kotori
      :limit     100
      :coll-path "providers/dmm/girls"
      :creds     (creds)}))
  (count products)

  (def products
    (select-scheduled-voice
     {:db    (db-prod)
      :info  (code->kotori "0031")
      :creds (creds)}))
  (count products)
  (select-while-url-exists products)
  )

