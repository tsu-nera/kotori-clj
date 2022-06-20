(ns kotori.procedure.dmm.doujin
  (:require
   [kotori.domain.dmm.genre.doujin :as genre]
   [kotori.domain.dmm.product
    :as d
    :refer [doujin-coll-path girls-coll-path]]
   [kotori.domain.kotori.core :refer [kotori->af-id]]
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

(defn crawl-girls-products!
  "女性向け"
  [{:keys [db] :as m}]
  (let [ts   (time/fs-now)
        docs (->> (lib/get-girls-products m)
                  (map lib/api->data)
                  (map (fn [m] (d/set-crawled-timestamp ts m)))
                  (map json/->json))]
    (doto db
      (product/save-products! girls-coll-path docs)
      (product/update-crawled-time! ts "doujin" genre/for-girl-id))
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
  (let [genre-ids (d/doc->genre-ids p)]
    (lib/tl? genre-ids)))

(defn bl-product? [p]
  (let [genre-ids (d/doc->genre-ids p)]
    (lib/bl? genre-ids)))

(defmulti make-strategy :code)

(defmethod make-strategy "0002" [_]
  [(filter voice-product?)
   (st/->st-include genre/chikubi-ids)])

(defmethod make-strategy "0026" [_]
  [(filter image-product?)
   (filter tl-product?)])

(defmethod make-strategy "0029" [_]
  [(filter image-product?)])

(defmethod make-strategy "0031" [_]
  [(filter voice-product?)
   (st/->st-exclude genre/chikubi-ids)])

(defmethod make-strategy :default [_]
  [])

;; TODO とりあえずやっつけで分岐するがあとでインタフェースで解決する.
(defn select-scheduled-image
  [{:keys [info db limit creds coll-path genre-id]
    :as   m
    :or   {limit 200}}]
  (let [products (lib/get-products {:genre-id genre-id
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
   '[firebase :refer [db db-prod]])

  (def cid "d_227233")
  (def resp (lib/get-product {:cid cid :creds (creds)}))
  (def resp (lib/get-products {:creds (creds) :hits 100}))

  (def urls (map #(get-in % [:imageURL :list]) resp))

  (def resp (crawl-product! {:db (db) :cid cid :creds (creds)}))

  (def products (crawl-products! {:db    (db-prod)
                                  :creds (creds)
                                  :limit 300}))

  (def girls (crawl-girls-products! {:db    (db-prod)
                                     :creds (creds)
                                     :limit 300}))

  (def products (crawl-voice-products! {:db    (db)
                                        :creds (creds)
                                        :limit 100}))

  (lib/get-products {:creds (creds)
                     :hits  200})

  (def products
    (select-scheduled-image
     {:db        (db-prod)
      :info      (code->kotori "0026")
      :limit     100
      :coll-path "providers/dmm/girls"
      :genre-id  genre/for-girl-id
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
