(ns kotori.procedure.dmm.anime
  (:require
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.genre.anime :as d]
   [kotori.lib.provider.dmm.product :as lib]
   [kotori.procedure.dmm.product :as product]
   [kotori.procedure.strategy.dmm :as st]))

(def floor (:anime dmm/floor))

(defn crawl-product! [{:as m}]
  (-> m
      (assoc :floor floor)
      (product/crawl-product! d/coll-path)))

(defn crawl-products! [{:as m}]
  (let [opts {:coll-path d/coll-path
              ;; いろいろ書かれてて複雑なので刈り取りは保留.
              :scrape?   false
              :floor     floor}]
    (product/crawl-products! (merge m opts))))

(defn select-scheduled-products
  [{:as m}]
  (st/select-scheduled-products
   (-> m (assoc :floor floor))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[devtools :refer [->screen-name kotori-info]]
           '[tools.dmm :refer [creds]]
           '[kotori.lib.kotori :refer [->next]]
           '[firebase :refer [db-prod db-dev db]])

  (def product (lib/get-anime {:creds (creds)
                               :cid   "h_1379jdxa57641"}))

  (def products (lib/get-products {:creds @creds
                                   :limit 10
                                   :floor "anime"}))

  (def resp (crawl-product! {:db    (db-prod)
                             :creds (creds)
                             :cid   "196glod00227"}))

  (def resp (crawl-products! {:db    (db)
                              :creds (creds)
                              :limit 300}))

  (def products
    (into []
          (select-scheduled-products
           {:db          (db)
            :limit       200
            :creds       (creds)
            :info        (kotori-info "0024")
            :screen-name (->screen-name "0024")})))
  (count products)

  (into [] (apply comp (st/make-strategy (kotori-info "0024"))) products)

  (map ->next products)
  )
