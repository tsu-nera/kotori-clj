(ns kotori.procedure.dmm.doujin
  (:require
   [kotori.domain.dmm.product
    :as d
    :refer [doujin-coll-path]
    :rename
    {doujin-coll-path coll-path}]
   [kotori.lib.json :as json]
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

(defn image-product? [p]
  (or (= "cg" (:format p))
      (= "comic" (:format p))))

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
        title      (:title doc)
        image-urls (lib/get-image-urls cid)]
    {:cid   cid
     :title title
     :urls  image-urls}))

(comment
  (require
   '[devtools :refer [kotori-info]]
   '[tools.dmm :refer [creds]]
   '[firebase :refer [db]])

  (def cid "d_227233")
  (def resp (lib/get-product {:cid cid :creds (creds)}))

  (def resp (crawl-product! {:db (db) :cid cid :creds (creds)}))

  (def products (crawl-products! {:db    (db)
                                  :creds (creds)
                                  :limit 10}))

  (def products
    (select-scheduled-image
     {:db    (db)
      :info  (kotori-info "0029")
      :limit 30
      :creds (creds)}))

  )
