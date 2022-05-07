(ns kotori.procedure.dmm.amateur
  (:require
   [kotori.domain.dmm.product
    :refer [amateur-coll-path]
    :rename
    {amateur-coll-path coll-path}]
   [kotori.domain.dmm.videoa
    :refer [amateur-genre-id]
    :rename {amateur-genre-id videoa-id}]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.product :as lib]
   [kotori.procedure.dmm.product :as product]))

;; 素人ジャンルはvideocに属するものとvideoaに属するジャンルがある.
;; 素人ジャンル作品として女優を利用していればvideoa,

(defn get-videoa-products [{:as params}]
  (let [opts {:floor      (:videoa api/floor)
              :article    (:genre api/article)
              :article_id videoa-id}]
    (lib/get-products (merge params opts))))

(defn crawl-product! [{:as m}]
  (-> m
      (assoc :floor (:videoc api/floor))
      (product/crawl-product! coll-path)))

(defn crawl-products! [{:keys [db] :as m}] nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[devtools :refer [env ->screen-name]]
           '[tools.dmm :refer [creds]]
           '[firebase :refer [db-prod db-dev db]])
  )

(comment
  (def product (lib/get-videoc {:creds @creds
                                :cid   "smuc029"}))
  (def product (lib/get-videoa {:creds @creds
                                :cid   "1kmhrs00044"}))
  )

(comment
  (def resp (crawl-product! {:db    @db
                             :creds @creds
                             :cid   "dots003"}))
  )
