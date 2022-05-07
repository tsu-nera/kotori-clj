(ns kotori.procedure.dmm.amateur
  (:require
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.product
    :refer [amateur-coll-path]
    :rename
    {amateur-coll-path coll-path}]
   [kotori.domain.dmm.videoa
    :refer [amateur-genre-id]
    :rename {amateur-genre-id videoa-id}]
   [kotori.lib.provider.dmm.product :as lib]
   [kotori.lib.time :as time]
   [kotori.procedure.dmm.product :as product]))

;; 素人ジャンルはvideocに属するものとvideoaに属するジャンルがある.
;; 素人ジャンル作品として女優を利用していればvideoa,

(defn get-videoa-products [{:as params}]
  (let [opts {:floor      (:videoa dmm/floor)
              :article    (:genre dmm/article)
              :article_id videoa-id}]
    (lib/get-products (merge params opts))))

(defn crawl-videoc-product! [{:as m}]
  (-> m
      (assoc :floor (:videoc dmm/floor))
      (product/crawl-product! coll-path)))

(defn crawl-products!
  [{:keys [db] :as m}]
  (let [floor    (:videoc dmm/floor)
        field-ts (:amateurs-crawled-time  dmm/field)
        ts       (time/fs-now)]
    (when-let [products (-> m
                            (assoc :floor floor)
                            (lib/get-products))]
      (doto db
        (product/save-products! coll-path products ts)
        (product/update-crawled-time! field-ts ts)
        (product/scrape-desc-if! coll-path field-ts floor))
      {:timestamp ts
       :count     (count products)
       :products  products})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[devtools :refer [env ->screen-name]]
           '[tools.dmm :refer [creds]]
           '[firebase :refer [db-prod db-dev db]])
  )

(comment
  (def product (lib/get-videoc {:creds (creds)
                                :cid   "smuc029"}))
  (def product (lib/get-videoa {:creds (creds)
                                :cid   "1kmhrs00044"}))
  )

(comment
  (def resp (crawl-videoc-product! {:db    (db)
                                    :creds (creds)
                                    :cid   "dots003"}))

  (def resp (crawl-products! {:db    (db)
                              :creds (creds)
                              :limit 10}))

  )
