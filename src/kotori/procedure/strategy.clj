(ns kotori.procedure.strategy
  "商品選択戦略"
  (:require
   [clojure.string :as str]
   [kotori.lib.firestore :as fs]
   [kotori.lib.time :as time]))

;; TODO 共通化
(def dmm-doc-path "providers/dmm")
(def products-path "providers/dmm/products")

(def vr-genre-ids
  #{6793 6925})

(def antisocial-genre-ids
  "Twitter的にダメなジャンル."
  #{4021 5015})

(def violent-genre-ids
  #{21 567 5059 6094 6953})

(def dirty-genre-ids
  #{4018 5007 5011 5012 5013 5014 5024 6151})

(def ng-genres
  (into #{} (concat
             vr-genre-ids
             antisocial-genre-ids
             violent-genre-ids
             dirty-genre-ids)))

(defn ng-genre? [id]
  (contains? ng-genres id))

(defn no-sample-movie? [product]
  (:no-sample-movie product))

(defn ng-product? [product]
  (some true? (map
               (comp ng-genre? #(get % "id"))
               (:genres product))))

(def st-exclude-no-sample-movie
  (remove no-sample-movie?))

(def st-exclude-ng-genres
  (remove ng-product?))

(def st-exclude-amateur
  (remove #(zero? (:actress-count %))))

(def st-exclude-omnibus
  (remove #(> (:actress-count %) 4)))

(def st-exclude-recently-tweeted
  "最終投稿から2ヶ月以上経過"
  (remove
   (fn [p]
     (let [past-time (time/date->weeks-ago 8)
           last-time (:last-tweet-time p)]
       (and last-time
            (time/after? (time/->tz-jst last-time) past-time))))))

(def st-already-tweeted
  (filter #(contains? % :last_tweet_id)))

(defn select-scheduled-products [{:keys [db limit] :or {limit 5}}]
  (let [last-crawled-time (fs/get-in db dmm-doc-path
                                     "products_crawled_time")
        st-last-crawled   (fs/query-filter
                           "last_crawled_time" last-crawled-time)
        products          (fs/get-docs
                           db products-path st-last-crawled)
        xstrategy         (comp
                           st-exclude-no-sample-movie
                           st-exclude-recently-tweeted
                           st-exclude-ng-genres
                           st-exclude-amateur
                           st-exclude-omnibus)]
    (->> (into [] xstrategy products)
         ;; sortはtransducerに組み込まないほうが楽.
         (sort-by :rank-popular)
         (take limit))))

(defn select-tweeted-products [{:keys [db limit] :or {limit 5}}]
  (let [q-already-tweeted (fs/query-exists "last_tweet_time" :desc)
        products          (fs/get-docs db products-path q-already-tweeted)
        ;; xstrategy         (comp
        ;;                    st-exclude-no-sample-movie
        ;;                    st-exclude-recently-tweeted
        ;;                    st-exclude-ng-genres
        ;;                    st-exclude-amateur
        ;;                    st-exclude-omnibus)
        ]
    (->> products
         ;; (into [] xstrategy)
         ;; sortはtransducerに組み込まないほうが楽.
         ;; (sort-by :rank-popular)
         (take limit))))

(defn ->next
  [product]
  (let [cid   (:cid product)
        title (:title product)]
    {:cid   cid
     :title title}))

(defn ->next-qvt
  [product]
  (let [cid             (:cid product)
        title           (:title product)
        tweet-id        (:last-tweet-id product)
        screen-name     (:last-tweet-name product)
        last-tweet-time (:last-tweet-time product)
        url             (str "https://twitter.com/" screen-name
                             "/status/" tweet-id "/video/1")]
    {:url             url
     :last-tweet-time last-tweet-time
     :cid             cid
     :title           title}))

(defn ->print
  [product]
  (let [raw       (-> product
                      (dissoc :legacy)
                      (dissoc :raw))
        cid       (:cid raw)
        title     (let [title (:title raw)]
                    (if (< (count title) 15)
                      title
                      (subs title 0 15)))
        actresses (str/join "," (map #(get % "name") (:actresses raw)))
        ;; genres  (str/join "," (map #(get % "name") (:genres raw)))
        ranking   (:rank-popular raw)]
    {:cid       cid
     :ranking   ranking
     :title     title
     :actresses actresses
     ;; :no-sample-movie (:no-sample-movie raw)
     ;; :genres  genres
     ;; :last-crawled-time (:last-crawled-time raw)
     ;; :raw             raw
     ;; :last-tweet-time (:last-tweet-time raw)
     }))

(defn select-next-product [{:keys [db]}]
  (->next (first (select-scheduled-products {:db db}))))

(defn select-next-qvt-product [{:keys [db]}]
  (->next-qvt (first (select-tweeted-products {:db db}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[firebase :refer [db-prod]])

  (def products
    (into []
          (select-tweeted-products {:db (db-prod) :limit 10})))

  (def qrt-products (map ->next-qvt products))
  (def product (select-next-qvt-product {:db (db-prod) :limit 10}))
  )

(comment
  ;;;;;;;;;;;
  (require '[firebase :refer [db-prod]])

  (def product (select-next-product {:db (db-prod)}))

  ;; cf. https://www.dmm.co.jp/digital/videoa/-/list/=/sort=ranking/
  (def products
    (into []
          (select-scheduled-products {:db (db) :limit 100})))

  (count products)
  (map ->next products)

  (def xst (comp
            st-exclude-ng-genres
            st-exclude-amateur
            st-exclude-omnibus
            (take 3)))

  (def result (into [] xst products))

  (map ->print (select-scheduled-products {:db (db-prod) :limit 20}))
 ;;;;;;;;;;;
  )

(comment
  (require '[devtools :refer [env db]])

  (def query (fs/query-limit 5))

  (fs/get-docs (db) "providers/dmm/products" query)
  )
