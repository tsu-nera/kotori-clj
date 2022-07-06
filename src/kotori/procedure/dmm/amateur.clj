(ns kotori.procedure.dmm.amateur
  (:require
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.genre.videoa
    :refer [amateur-genre-id]
    :rename {amateur-genre-id videoa-id}]
   [kotori.domain.dmm.product
    :refer [amateur-coll-path]
    :rename
    {amateur-coll-path coll-path}]
   [kotori.lib.provider.dmm.product :as lib]
   [kotori.procedure.dmm.product :as product]
   [kotori.procedure.strategy.dmm :as st]))

(def floor (:videoc dmm/floor))

;; 素人ジャンルはvideocに属するものとvideoaに属するジャンルがある.
;; 素人ジャンル作品として女優を利用していればvideoa,
(defn get-videoa-products [{:as params}]
  (let [opts {:floor      (:videoa dmm/floor)
              :article    (:genre dmm/article)
              :article_id videoa-id}]
    (lib/get-products (merge params opts))))

(defn get-videoc-products [{:as params}]
  (let [opts {:floor (:videoc dmm/floor)}]
    (lib/get-products (merge params opts))))

(defn crawl-videoc-product! [{:as m}]
  (-> m
      (assoc :floor (:videoc dmm/floor))
      (product/crawl-product! coll-path)))

(defn scrape-videoc-page! [{:as m}]
  (-> m
      (assoc :floor (:videoc dmm/floor))
      (product/scrape-page! coll-path)))

(defn crawl-products! [{:as m}]
  (let [opts {:coll-path coll-path
              :floor     floor}]
    (product/crawl-products! (merge m opts))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[devtools :refer [env ->screen-name code->kotori]]
           '[tools.dmm :refer [creds]]
           '[kotori.lib.kotori :refer [->next] :as k]
           '[firebase :refer [db-prod db-dev db]])
  )

(comment
  (def product (lib/get-videoc {:creds (creds)
                                :cid   "srsy030"}))

  (def product (lib/get-videoa {:creds (creds)
                                :cid   "1kmhrs00044"}))
  )

(comment
  (def videocs (get-videoc-products {:creds      (creds)
                                     :article    "genre"
                                     :article_id 8510}))

  )

(comment
  (def resp (crawl-videoc-product! {:db    (db-prod)
                                    :creds (creds)
                                    :cid   "shinki066"}))

  (def resp (scrape-videoc-page! {:db  (db-prod)
                                  :cid "shinki066"}))

  (def resp (crawl-products! {:db       (db-prod)
                              :creds    (creds)
                              :genre-id 4031
                              :limit    100}))

  (def info (code->kotori "0027"))
  (def products
    (st/select-scheduled-products
     {:db    (db-prod)
      :creds (creds)
      :info  info}))
  (count products)

  (def ret (map :description (map ->next products)))

  (def product (first products))
  )
