(ns kotori.procedure.dmm.anime
  (:require
   [kotori.domain.dmm.anime :as d]
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.product
    :refer [anime-coll-path]
    :rename
    {anime-coll-path coll-path}]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.product :as lib]
   [kotori.lib.time :as time]
   [kotori.procedure.dmm.product :as product]
   [kotori.procedure.strategy.dmm :as st]))

(def floor (:anime api/floor))

(defn crawl-product! [{:as m}]
  (-> m
      (assoc :floor floor)
      (product/crawl-product! coll-path)))

(defn crawl-products!
  [{:keys [db] :as m}]
  (let [field-ts (:animes-crawled-time dmm/field)
        ts       (time/fs-now)]
    (when-let [products (-> m
                            (assoc :floor floor)
                            (lib/get-products))]
      (doto db
        (product/save-products! coll-path products ts)
        (product/update-crawled-time! field-ts ts)
        ;; いろいろ書かれてて複雑なので刈り取りは保留.
        ;; (product/scrape-desc-if! coll-path field-ts)
        )
      {:timestamp ts
       :count     (count products)
       :products  products})))

(defn select-scheduled-products [{:as m :keys [db limit] :or {limit 5}}]
  (let [st-exclude-ng-genres (st/make-st-exclude-ng-genres d/ng-genres)
        xst                  [st-exclude-ng-genres
                              st/st-exclude-no-samples]
        params               (st/assoc-last-crawled-time
                              m db (:animes-crawled-time dmm/field))
        products             (st/select-scheduled-products-with-xst
                              params xst coll-path)]
    (->> products
         (sort-by :rank-popular)
         (take limit))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[devtools :refer [->screen-name]]
           '[tools.dmm :refer [creds]]
           '[firebase :refer [db-prod db-dev db]])

  (def product (lib/get-anime {:creds (creds)
                               :cid   "196glod00227"}))

  (def products (lib/get-products {:creds @creds
                                   :limit 10
                                   :floor "anime"}))

  (def resp (crawl-product! {:db    (db)
                             :creds (creds)
                             :cid   "196glod00227"}))

  (def resp (crawl-products! {:db    (db)
                              :creds (creds)
                              :limit 10}))

  (def products
    (into []
          (select-scheduled-products
           {:db          (db-prod)
            :limit       5
            :screen-name (->screen-name "0024")})))
  )
