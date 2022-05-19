(ns kotori.procedure.dmm.amateur
  (:require
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.genre.videoa
    :refer [amateur-genre-id]
    :rename {amateur-genre-id videoa-id}]
   [kotori.domain.dmm.genre.videoc :as d]
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

(defn crawl-videoc-product! [{:as m}]
  (-> m
      (assoc :floor (:videoc dmm/floor))
      (product/crawl-product! coll-path)))

(defn scrape-videoc-page! [{:as m}]
  (-> m
      (assoc :floor (:videoc dmm/floor))
      (product/scrape-page coll-path)))

(defn crawl-products! [{:as m}]
  (let [opts {:coll-path coll-path
              :floor     floor}]
    (product/crawl-products! (merge m opts))))

(defn select-scheduled-products [{:as m :keys [db limit] :or {limit 5}}]
  (let [st-exclude-ng-genres (st/make-st-exclude-ng-genres d/ng-genres)
        xst                  [st-exclude-ng-genres
                              st/st-exclude-no-samples]
        ts                   (st/get-last-crawled-time db floor "default")
        params               (assoc m :last-crawled-time ts)
        products             (st/select-scheduled-products-with-xst-deplicated
                              params xst coll-path)]
    (->> products
         (sort-by :rank-popular)
         (take limit))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[devtools :refer [env ->screen-name]]
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
  (def resp (crawl-videoc-product! {:db    (db-prod)
                                    :creds (creds)
                                    :cid   "shinki066"}))

  (def resp (scrape-videoc-page! {:db  (db-prod)
                                  :cid "shinki066"}))

  ;; (require '[kotori.lib.provider.dmm.public :as public])
  ;; (public/get-page {:db    (db-prod)
  ;;                   :floor "videoc"
  ;;                   :cid   "erk022"})

  (def resp (crawl-products! {:db    (db-prod)
                              :creds (creds)
                              :limit 300}))

  (def products
    (select-scheduled-products
     {:db          (db-prod)
      :limit       100
      :screen-name (->screen-name "0027")}))

  (count products)

  (def ret (map :description (map ->next products)))

  (def product (first products))
  )
