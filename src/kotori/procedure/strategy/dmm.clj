(ns kotori.procedure.strategy.dmm
  "商品選択戦略"
  (:require
   [clojure.string :as str]
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.product :as product]
   [kotori.lib.firestore :as fs]
   [kotori.lib.kotori :as lib]
   [kotori.lib.time :as time]))

(def amateur-genre-ids
  #{;
    4024 ; 素人
    4006 ; ナンパ
    6002 ; ハメ撮り
    })

(def vr-genre-ids
  #{6793 6925})

(def antisocial-genre-ids
  "Twitter的にダメそうなジャンル."
  #{;
    4021 ; 盗撮・のぞき
    5015 ; ドラッグ
    })

(def violent-genre-ids
  #{21 567 5059 6094 6953})

(def dirty-genre-ids
  #{4018 5007 5011 5012 5013 5014 5024 6151})

(def trans-genre-ids
  #{3036 4015})

(def ng-genres
  (into #{} (concat
             vr-genre-ids
             antisocial-genre-ids
             violent-genre-ids
             dirty-genre-ids
             trans-genre-ids)))

(defn ng-genre? [id]
  (contains? ng-genres id))

(defn no-sample-movie? [product]
  (:no-sample-movie product))

(defn no-sample-image? [product]
  (:no-sample-image product))

(defn ng-product? [product]
  (some true? (map
               (comp ng-genre? #(get % "id"))
               (:genres product))))

(def st-exclude-no-samples
  (remove #(or (no-sample-movie? %)
               (no-sample-image? %))))

(def st-exclude-ng-genres
  (remove ng-product?))

(defn no-actress? [product]
  (zero? (:actress-count product)))

(defn ->genre-ids [product]
  (->> product
       :genres
       (map #(get % "id"))
       (into [])))

(defn contains-genre? [genre-id-set product]
  (some true?
        (map #(contains? genre-id-set %) (->genre-ids product))))

(def st-exclude-amateur
  (remove #(or (no-actress? %)
               (contains-genre? amateur-genre-ids %))))

(def st-include-amateur
  (filter #(or (no-actress? %)
               (contains-genre? amateur-genre-ids %))))

(def st-exclude-omnibus
  (remove #(> (:actress-count %) 4)))

(defn- make-st-exclude-recently-tweeted
  "最終投稿からXdays以上経過"
  [self? days]
  (remove
   (fn [p]
     (let [past-time        (time/date->days-ago days)
           last-time        (:last-tweet-time p)
           last-screen-name (:last-tweet-name p)]
       (and last-time
            (self? last-screen-name)
            (time/after? (time/->tz-jst last-time) past-time))))))

(defn make-st-exclude-recently-tweeted-self
  [target-screen-name days]
  (make-st-exclude-recently-tweeted #(= target-screen-name %) days))

(defn make-st-exclude-recently-tweeted-others
  [target-screen-name days]
  (make-st-exclude-recently-tweeted #(not= target-screen-name %) days))

(defn make-st-exclude-recently-quoted
  "最終引用投稿からX日以上経過"
  [days]
  (remove
   (fn [p]
     (let [past-time (time/date->days-ago days)
           last-time (:last-quoted-time p)]
       (and last-time
            (time/after? (time/->tz-jst last-time) past-time))))))

(defn make-st-exclude-last-quoted-self
  "最終引用が自分だったら除外"
  [screen-name]
  (remove
   (fn [p]
     (let [last-quoted-name (:last-quoted-name p)]
       (and last-quoted-name
            (= last-quoted-name screen-name))))))

(def st-already-tweeted
  (filter #(contains? % :last_tweet_id)))

(def st-skip-debug
  (remove #(get % :debug)))

(defn select-scheduled-products-with-xst [{:keys [db limit screen-name]
                                           :or   {limit 5}} xst]
  (let [last-crawled-time (fs/get-in db dmm/doc-path
                                     "products_crawled_time")
        st-last-crawled   (fs/query-filter
                           "last_crawled_time"
                           last-crawled-time)
        st-exclude-recently-tweeted-self
        (make-st-exclude-recently-tweeted-self screen-name 28)
        st-exclude-recently-tweeted-others
        (make-st-exclude-recently-tweeted-others screen-name 14)
        products          (fs/get-docs
                           db product/coll-path st-last-crawled)
        xstrategy         (apply comp
                                 st-skip-debug
                                 st-exclude-no-samples
                                 st-exclude-recently-tweeted-self
                                 st-exclude-recently-tweeted-others
                                 xst)]
    (->> products
         (into [] xstrategy)
         ;; sortはtransducerに組み込まないほうが楽.
         (sort-by :rank-popular)
         (take limit))))

(defn select-scheduled-products [{:as params}]
  (let [xst [st-exclude-ng-genres
             st-exclude-amateur
             st-exclude-omnibus]]
    (select-scheduled-products-with-xst params xst)))

(defn select-scheduled-amateurs [{:as params}]
  (let [xst [st-exclude-ng-genres
             st-exclude-omnibus
             st-include-amateur]]
    (select-scheduled-products-with-xst params xst)))

(defn select-tweeted-products [{:keys [db limit screen-name]
                                :or   {limit 5}}]
  {:pre [(string? screen-name)]}
  (let [q-already-tweeted
        ;; 42日前から21日分を候補にする.
        (fs/query-between 49 21 "last_tweet_time")
        ;; 一応個数制限
        ;; 200だと7s,300だと10sなので150程度に調整.
        ;; その分query-betweenの期間を拡張して様子見.
        q-limit                     (fs/query-limit 300)
        xquery                      (fs/make-xquery [q-already-tweeted
                                                     q-limit])
        products                    (fs/get-id-doc-map
                                     db
                                     product/coll-path xquery)
        st-exclude-last-quoted-self (make-st-exclude-last-quoted-self
                                     screen-name)
        st-exclude-recently-tweeted-others
        (make-st-exclude-recently-tweeted-others screen-name 14)
        st-exclude-recently-quoted  (make-st-exclude-recently-quoted 4)
        xstrategy                   (comp
                                     st-skip-debug
                                     st-exclude-recently-tweeted-others
                                     st-exclude-last-quoted-self
                                     st-exclude-recently-quoted)]
    (->> products
         ;; cidをidには入れたけどdocに入れ忘れたのでhotfix
         ;; keyを :cidとしてvalに取り付ける.これは特別対応.
         (map (juxt key val))
         (map (fn [[k v]] (assoc v :cid k)))
         (into [] xstrategy)
         ;; 新しい順に並び替える
         (sort-by :last-tweet-time #(compare %2 %1))
         (take limit))))

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
    {:cid             cid
     :ranking         ranking
     :title           title
     :actresses       actresses
     ;; :no-sample-movie (:no-sample-movie raw)
     ;; :genres  genres
     ;; :last-crawled-time (:last-crawled-time raw)
     ;; :raw             raw
     :last-tweet-id   (:last-tweet-id raw)
     :last-tweet-name (:last-tweet-name raw)
     :last-tweet-time (:last-tweet-time raw)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[firebase :refer [db-prod db-dev db]]
           '[devtools :refer [->screen-name env]])
  )

(comment

  (def products
    (into [] (select-tweeted-products
              {:db          (db-prod) :limit 200
               :screen-name (->screen-name "0019")})))

  (count products)
  (map ->print products)
  )

(comment
  ;;;;;;;;;;;
  (def screen-name (->screen-name "0001"))

  ;; cf. https://www.dmm.co.jp/digital/videoa/-/list/=/sort=ranking/
  (def products
    (into []
          (select-scheduled-products {:db          (db-prod)
                                      :limit       20
                                      :screen-name screen-name})))
  (def descs (map :description products))

  (count products)

  (def screen-name (->screen-name "0027"))
  (def amateurs
    (into []
          (select-scheduled-amateurs {:db          (db-prod)
                                      :limit       5
                                      :screen-name screen-name})))
  (count amateurs)

  (def product (nth products 27))
  (def next (lib/->next product))
  (def desc (:description product))
  (lib/desc->trimed desc)
  (lib/desc->headline desc)

  (def genre-ids (->genre-ids product))
  (some true? (map #(contains? amateur-genre-ids %) genre-ids))

  (defn ng-product? [product]
    (some true? (map
                 (comp ng-genre? #(get % "id"))
                 (:genres product))))

  (def xst (comp
            st-exclude-ng-genres
            st-exclude-amateur
            st-exclude-omnibus
            (take 3)))

  (def result (into [] xst products))

  (map ->print (select-scheduled-products {:db (db-prod) :limit 20}))

 ;;;;;;;;;;;
  )

