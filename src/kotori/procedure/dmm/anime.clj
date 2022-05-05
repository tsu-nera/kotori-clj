(ns kotori.procedure.dmm.anime
  (:require
   [kotori.domain.dmm.product
    :refer [anime-coll-path]
    :rename {anime-coll-path coll-path}]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.time :as time]
   [kotori.procedure.dmm.product :as product]))

(defn get-product [{:as params :keys [cid]}]
  {:pre [(string? cid)]}
  (let [opts {:floor (:anime api/floor)}]
    (product/get-product (merge params opts))))

(defn get-products [{:as params}]
  (let [opts {:floor (:anime api/floor)}]
    (product/get-products (merge params opts))))

(defn crawl-product!
  ([{:keys [db cid] :as m}]
   {:pre [(string? cid)]}
   (let [ts      (time/fs-now)
         product (get-product m)]
     (product/save-product! db coll-path product ts))))

(comment
  (require '[devtools :refer [env ->screen-name]]
           '[firebase :refer [db-prod db-dev db]])

  (def product (get-product {:env (env)
                             :cid "196glod00227"}))

  (def products (get-products {:env (env) :limit 10}))

  (def resp (crawl-product! {:db  (db)
                             :env (env)
                             :cid "196glod00227"}))

  (def resp (crawl-products! {:db    (db)
                              :env   (env)
                              :limit 10}))


  (def products
    (into []
          (select-scheduled-products
           {:db          (db)
            :limit       10
            :screen-name (->screen-name "0024")})))
  )
