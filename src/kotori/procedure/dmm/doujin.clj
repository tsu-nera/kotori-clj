(ns kotori.procedure.dmm.doujin
  (:require
   [kotori.domain.dmm.product
    :refer [doujin-coll-path]
    :rename
    {doujin-coll-path coll-path}]
   [kotori.lib.provider.dmm.doujin :as lib]
   [kotori.lib.time :as time]
   [kotori.procedure.dmm.product :as product]))

(defn crawl-product! [{:keys [db] :as m}]
  (when-let [product (lib/get-product m)]
    (product/save-product! db
                           coll-path
                           product
                           (time/fs-now))))

(defn crawl-products! [{:keys [db] :as m}]
  (let [ts       (time/fs-now)
        products (lib/get-products m)]
    (doto db
      (product/save-products! coll-path products ts)
      (product/update-crawled-time! ts "doujin" nil))
    {:timestamp ts
     :count     (count products)
     :products  products}))

#_(defn select-scheduled-products
    [{:as m}]
    (st/select-scheduled-products
     (-> m (assoc :floor floor))))

(comment
  (require '[tools.dmm :refer [creds]]
           '[firebase :refer [db]])

  (def cid "d_227233")
  (def resp (lib/get-product {:cid cid :creds (creds)}))

  (def resp (crawl-product! {:db (db) :cid cid :creds (creds)}))

  (def products (crawl-products! {:db    (db)
                                  :creds (creds)
                                  :limit 10}))

  )
