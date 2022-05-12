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
   [kotori.domain.dmm.videoc :as d]
   [kotori.lib.provider.dmm.product :as lib]
   [kotori.lib.time :as time]
   [kotori.procedure.dmm.product :as product]
   [kotori.procedure.strategy.dmm :as st]))

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

(defn select-scheduled-products [{:as m :keys [db limit] :or {limit 5}}]
  (let [st-exclude-ng-genres (st/make-st-exclude-ng-genres d/ng-genres)
        xst                  [st-exclude-ng-genres
                              st/st-exclude-no-samples]
        params               (st/assoc-last-crawled-time
                              m db (:amateurs-crawled-time dmm/field))
        products             (st/select-scheduled-products-with-xst
                              params xst coll-path)]
    (->> products
         (sort-by :rank-popular)
         (take limit)
         (into []))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[devtools :refer [env ->screen-name]]
           '[tools.dmm :refer [creds]]
           '[kotori.lib.kotori :refer [->next] :as k]
           '[firebase :refer [db-prod db-dev db]])
  )

(comment
  (def product (lib/get-videoc {:creds (creds)
                                :cid   "ttk005"}))

  (def product (lib/get-videoa {:creds (creds)
                                :cid   "1kmhrs00044"}))
  )

(comment
  (def resp (crawl-videoc-product! {:db    (db-prod)
                                    :creds (creds)
                                    :cid   "ttk005"}))

  (def resp (scrape-videoc-page! {:db  (db-prod)
                                  :cid "ttk005"}))

  ;; (require '[kotori.lib.provider.dmm.public :as public])
  ;; (public/get-page {:db    (db-prod)
  ;;                   :floor "videoc"
  ;;                   :cid   "erk022"})

  (def resp (crawl-products! {:db    (db)
                              :creds (creds)
                              :limit 120}))

  (def products
    (select-scheduled-products
     {:db          (db-prod)
      :limit       30
      :screen-name (->screen-name "0027")}))
  (def ret (map :title (map ->next products)))

  (def product (first products))
  )
