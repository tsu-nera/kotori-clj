(ns kotori.procedure.dmm.vr
  (:require
   [kotori.domain.dmm.genre
    :refer [vr-only-id]
    :rename {vr-only-id genre-id}]
   [kotori.domain.dmm.product
    :refer [vr-coll-path]
    :rename {vr-coll-path coll-path}]
   [kotori.lib.time :as time]
   [kotori.procedure.dmm.product :as product]))

(defn get-products [{:as params}]
  (let [vr-option {:article    "genre"
                   :article_id genre-id}]
    (product/get-products (merge params vr-option))))

(defn crawl-product! [{:as m}]
  (product/crawl-product! m coll-path))

(defn crawl-products!
  [{:keys [db] :as m}]
  (let [timestamp-key "vrs_crawled_time"
        ts            (time/fs-now)]
    (when-let [products (get-products m)]
      (doto db
        (product/save-products! coll-path products ts)
        (product/update-crawled-time! timestamp-key ts)
        (product/scrape-desc-if! coll-path timestamp-key))
      {:timestamp ts
       :count     (count products)
       :products  products})))

(comment
  (require '[devtools :refer [env]]
           '[firebase :refer [db-prod db-dev db]])

  (def products (get-products {:env (env) :limit 10}))

  (def resp (crawl-product! {:db  (db)
                             :env (env)
                             :cid "bibivr00059"}))

  (def resp (crawl-products! {:db    (db-dev)
                              :env   (env)
                              :limit 10}))
  )
