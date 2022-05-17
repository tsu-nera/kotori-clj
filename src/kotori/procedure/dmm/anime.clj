(ns kotori.procedure.dmm.anime
  (:require
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.genre.anime :as d]
   [kotori.domain.dmm.product
    :refer [anime-coll-path]
    :rename
    {anime-coll-path coll-path}]
   [kotori.lib.provider.dmm.product :as lib]
   [kotori.procedure.dmm.product :as product]
   [kotori.procedure.strategy.dmm :as st]))

(def floor (:anime dmm/floor))

(defn crawl-product! [{:as m}]
  (-> m
      (assoc :floor floor)
      (product/crawl-product! coll-path)))

(defn crawl-products! [{:as m}]
  (let [opts {:coll-path coll-path
              ;; いろいろ書かれてて複雑なので刈り取りは保留.
              :scrape?   false
              :floor     floor}]
    (product/crawl-products! (merge m opts))))

(defn select-scheduled-products [{:as m :keys [db limit] :or {limit 5}}]
  (let [st-exclude-ng-genres (st/make-st-exclude-ng-genres d/ng-genres)
        xst                  [#_st/st-exclude-no-genres
                              st-exclude-ng-genres
                              st/st-exclude-no-samples]
        ts                   (st/get-last-crawled-time db floor "default")
        params               (assoc m :last-crawled-time ts)
        products             (st/select-scheduled-products-with-xst-deplicated
                              params xst coll-path)]
    (->> products
         (sort-by :rank-popular)
         (take limit))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[devtools :refer [->screen-name]]
           '[tools.dmm :refer [creds]]
           '[kotori.lib.kotori :refer [->next]]
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

  (map ->next products)
  )
