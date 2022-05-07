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
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.product :as lib]
   [kotori.lib.time :as time]
   [kotori.procedure.dmm.product :as product]))

;; 素人ジャンルはvideocに属するものとvideoaに属するジャンルがある.
;; 素人ジャンル作品として女優を利用していればvideoa,

(defn get-videoa-products [{:as params}]
  (let [opts {:floor      (:videoa api/floor)
              :article    (:genre api/article)
              :article_id videoa-id}]
    (lib/get-products (merge params opts))))

(defn crawl-videoc-product! [{:as m}]
  (-> m
      (assoc :floor (:videoc api/floor))
      (product/crawl-product! coll-path)))

;; TODO 素人動画はタイトルが名前になっているのでそのままでは不十分
;; descriptionの刈り取りは必須
(defn crawl-products!
  [{:keys [db] :as m}]
  (let [field-ts (:amateurs-crawled-time  dmm/field)
        ts       (time/fs-now)]
    (when-let [products (-> m
                            (assoc :floor (:videoc api/floor))
                            (lib/get-products))]
      (doto db
        (product/save-products! coll-path products ts)
        (product/update-crawled-time! field-ts ts))
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
