(ns kotori.procedure.dmm.doujin
  (:require
   [kotori.domain.dmm.product
    :as d
    :refer [doujin-coll-path]
    :rename
    {doujin-coll-path coll-path}]
   [kotori.lib.json :as json]
   [kotori.lib.kotori :refer [ng->ok]]
   [kotori.lib.provider.dmm.doujin :as lib]
   [kotori.lib.time :as time]
   [kotori.procedure.dmm.product :as product]
   [kotori.procedure.strategy.dmm :as st]))

(defn crawl-product! [{:keys [db] :as m}]
  (when-let [product (lib/get-product m)]
    (product/save-product! db
                           coll-path
                           product
                           (time/fs-now))))

(defn crawl-products! [{:keys [db] :as m}]
  (let [ts   (time/fs-now)
        docs (->> (lib/get-products m)
                  (map lib/api->data)
                  (map (fn [m] (d/set-crawled-timestamp ts m)))
                  (map json/->json))]
    (doto db
      (product/save-products! coll-path docs)
      (product/update-crawled-time! ts "doujin" nil))
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
      (product/save-products! coll-path docs)
      (product/update-crawled-time! ts "doujin" nil))
    {:timestamp ts
     :count     (count docs)
     :products  docs}))

(defn image-product? [p]
  (or (= "cg" (:format p))
      (= "comic" (:format p))))

(defn voice-product? [p]
  (= "voice" (:format p)))

(defn select-scheduled-image
  [{:keys [info db limit creds]
    :as   m
    :or   {limit 200}}]
  (let [products (lib/get-products {:creds creds
                                    :limit limit})
        xst      [(filter image-product?)]
        doc-ids  (map :content_id products)]
    (->> (st/select-scheduled-products-with-xst
          m xst coll-path doc-ids)
         (take limit))))

(defn select-next-image [{:as params}]
  (let [doc        (first (select-scheduled-image params))
        cid        (:cid doc)
        title      (-> (:title doc) ng->ok)
        image-urls (lib/get-image-urls cid)]
    {:cid           cid
     :title         title
     :affiliate-url (:affiliate-url doc)
     :urls          image-urls}))

(defn select-scheduled-voice
  [{:keys [info db limit creds]
    :as   m
    :or   {limit 200}}]
  (let [products (lib/get-voice-products {:creds creds
                                          :limit limit})
        xst      [(filter voice-product?)]
        doc-ids  (map :content_id products)]
    (->> (st/select-scheduled-products-with-xst
          m xst coll-path doc-ids)
         (take limit))))

(defn select-next-voice [{:as params}]
  (let [doc        (first (select-scheduled-voice params))
        cid        (:cid doc)
        title      (-> (:title doc) ng->ok)
        voice-urls (lib/get-voice-urls cid)]
    {:cid           cid
     :title         title
     :affiliate-url (:affiliate-url doc)
     :urls          voice-urls}))

(comment
  (require
   '[devtools :refer [kotori-info]]
   '[tools.dmm :refer [creds]]
   '[firebase :refer [db]])

  (def cid "d_227233")
  (def resp (lib/get-product {:cid cid :creds (creds)}))
  (def resp (lib/get-products {:creds (creds) :hits 100}))

  (def urls (map #(get-in % [:imageURL :list]) resp))

  (def resp (crawl-product! {:db (db) :cid cid :creds (creds)}))

  (def products (crawl-products! {:db    (db)
                                  :creds (creds)
                                  :limit 300}))

  (def products (crawl-voice-products! {:db    (db)
                                        :creds (creds)}))


  (def products
    (select-scheduled-image
     {:db    (db)
      :info  (kotori-info "0029")
      :limit 30
      :creds (creds)}))


  (def products
    (select-scheduled-voice
     {:db    (db)
      :info  (kotori-info "0003")
      :limit 30
      :creds (creds)}))
  )
