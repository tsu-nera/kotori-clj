(ns kotori.procedure.strategy.dmm
  "商品選択戦略"
  (:require
   [clojure.string :as str]
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.genre.videoa :as videoa]
   [kotori.domain.dmm.product :as product]
   [kotori.domain.kotori :refer [guest-user]]
   [kotori.lib.firestore :as fs]
   [kotori.lib.kotori :as lib]
   [kotori.lib.provider.dmm.product :as lib-dmm]
   [kotori.lib.time :as time]))

(defn make-st-exclude-ng-genres [ids]
  (remove
   #(some true? (map
                 (fn [genre]
                   (let [id (get genre "id")]
                     (contains? ids id)))
                 (:genres %)))))

(def st-exclude-ng-genres
  (make-st-exclude-ng-genres videoa/ng-genres))

(defn no-genres? [product]
  (nil? (:genres product)))

(def st-exclude-no-genres
  (remove #(no-genres? %)))

(defn no-sample-movie? [product]
  (:no-sample-movie product))

(defn sample-movie? [product]
  (not (:no-sample-movie product)))

(defn no-sample-image? [product]
  (:no-sample-image product))

(def st-exclude-no-image
  (remove #(no-sample-image? %)))

(def st-exclude-movie
  (remove #(sample-movie? %)))

(def st-exclude-no-samples
  (remove #(or (no-sample-movie? %)
               (no-sample-image? %))))

(defn no-actress? [product]
  (let [count (:actress-count product)]
    (or (nil? count) (zero? count))))

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
               (contains-genre? videoa/amateur-ids %))))

(def st-include-amateur
  (filter #(or (no-actress? %)
               (contains-genre? videoa/amateur-ids %))))

(def st-exclude-omnibus
  (remove #(> (:actress-count %) 4)))

(defn ->st-include [genre-ids]
  (filter #(contains-genre? genre-ids %)))

(defn ->st-exclude [genre-ids]
  (remove #(contains-genre? genre-ids %)))

(def st-include-vr
  (filter #(contains-genre? videoa/vr-ids %)))

(def st-exclude-vr
  (remove #(contains-genre? videoa/vr-ids %)))

(defn recently-tweeted? [p days]
  (let [past-time (time/date->days-ago days)
        last-time (:last-tweet-time p)]
    (and last-time
         (time/after? (time/->tz-jst last-time) past-time))))

(defn- make-st-exclude-recently-tweeted
  "最終投稿からXdays以上経過"
  ([days]
   (remove #(recently-tweeted? % days)))
  ([days pred-self?]
   (remove
    (fn [p]
      (let [last-screen-name (:last-tweet-name p)]
        (and (pred-self? last-screen-name)
             (recently-tweeted? p days)))))))

(defn make-st-exclude-recently-tweeted-self
  [days target-screen-name]
  (make-st-exclude-recently-tweeted days #(= target-screen-name %)))

(defn make-st-exclude-recently-tweeted-others
  [days target-screen-name]
  (make-st-exclude-recently-tweeted days #(not= target-screen-name %)))

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

(def st-skip-ignore
  (remove #(get % :ignore)))

(def st-skip-not-yet-crawled
  (remove #(nil? (get % :cid))))

(def st-skip-not-yet-scraped
  (remove #(nil? (get % :description))))

(defn select-scheduled-products-with-xst-deplicated
  [{:keys [db screen-name last-crawled-time]} xst coll-path]
  (let [st-last-crawled (fs/query-filter
                         "last_crawled_time"
                         last-crawled-time)
        st-exclude-recently-tweeted-self
        (make-st-exclude-recently-tweeted-self 28 screen-name)
        st-exclude-recently-tweeted-others
        (make-st-exclude-recently-tweeted-others 14 screen-name)
        products        (fs/get-docs
                         db coll-path st-last-crawled)
        xstrategy       (apply comp
                               st-skip-debug
                               st-exclude-recently-tweeted-self
                               st-exclude-recently-tweeted-others
                               xst)]
    (->> products
         (into [] xstrategy))))

(defn get-last-crawled-time [db floor genre-id]
  (-> db
      (fs/get-in dmm/doc-path :last-crawled-time)
      (get-in [floor "genres" (str genre-id) "timestamp"])))

(defn assoc-last-crawled-time [m db floor genre-id]
  (let [last-crawled-time (get-last-crawled-time db floor genre-id)]
    (assoc m :last-crawled-time last-crawled-time)))

(defmulti make-strategy :code)

(defmethod make-strategy "0001" [_]
  [st-exclude-ng-genres
   st-exclude-no-samples
   st-exclude-vr
   st-exclude-amateur
   st-exclude-omnibus])

(defmethod make-strategy "0009" [_]
  [st-exclude-ng-genres
   st-exclude-no-samples
   st-exclude-vr
   st-exclude-omnibus
   st-include-amateur])

(defmethod make-strategy "0010" [_]
  [st-exclude-ng-genres
   st-exclude-no-samples
   st-exclude-vr
   st-exclude-amateur
   st-exclude-omnibus
   (->st-include videoa/fat-ids)])

(defmethod make-strategy :default [_]
  [st-exclude-ng-genres
   st-exclude-no-samples
   st-exclude-vr
   st-exclude-amateur
   st-exclude-omnibus])

(defn select-scheduled-products-with-xst
  [{:keys [db]} xst coll-path doc-ids]
  (let [st-exclude-recently-tweeted
        (make-st-exclude-recently-tweeted 28)
        products  (fs/get-docs-by-ids db coll-path doc-ids)
        xstrategy (apply comp
                         ;; FIXME crawlとscrapingがまだの場合の検討
                         st-skip-not-yet-crawled
                         st-skip-not-yet-scraped
                         st-skip-debug
                         st-skip-ignore
                         st-exclude-recently-tweeted
                         xst)]
    (->> products
         (into [] xstrategy))))

(defn select-scheduled-products
  [{:keys [info db limit creds genre-id]
    :as   m :or {limit 200}}]
  (let [xst      (make-strategy info)
        products (lib-dmm/get-products {:genre-id genre-id
                                        :creds    creds
                                        :limit    limit})
        doc-ids  (map :content_id products)]
    (->> (select-scheduled-products-with-xst
          m xst product/coll-path doc-ids)
         (take limit))))

(defn select-tweeted-products [{:keys [db limit screen-name]
                                :or   {limit       5
                                       screen-name guest-user}}]
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
        st-exclude-recently-quoted  (make-st-exclude-recently-quoted 4)
        xstrategy                   (comp
                                     st-skip-debug
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
           '[tools.dmm :refer [creds]]
           '[devtools :refer [->screen-name env kotori-info]])
  )

(comment

  (def products
    (into [] (select-tweeted-products
              {:db          (db-prod) :limit 200
               :screen-name (->screen-name "0018")})))

  (count products)
  (map ->print products)
  )

(comment
  ;;;;;;;;;;;
  (def info (kotori-info "0001"))
  ;; cf. https://www.dmm.co.jp/digital/videoa/-/list/=/sort=ranking/
  (def products
    (into []
          (select-scheduled-products
           {:db          (db-dev)
            :creds       (creds)
            :info        info
            :limit       100
            :screen-name (:screen-name info)})))
  (def descs (map :description products))

  (count products)

  (def info (kotori-info "0009"))
  (def amateurs
    (into []
          (select-scheduled-products
           {:db          (db-prod)
            :creds       (creds)
            :info        info
            :limit       100
            :genre-id    4024
            :screen-name (:screen-name info)})))
  (count amateurs)

  (fs/get-in (db-prod) dmm/doc-path "amateurs_crawled_time")

  (def product (nth products 27))
  (def next (lib/->next product))
  (def desc (:description product))
  (lib/desc->trimed desc)
  (lib/desc->headline desc)
  )

(comment
  (def resp (lib-dmm/get-by-genre {:genre-id 2007
                                   :creds    (creds)
                                   :limit    30}))
  (def resp (lib-dmm/get-by-genre {:creds (creds)
                                   :limit 30}))


  (def cids (map :content_id resp))

  (def products (fs/get-docs-by-ids (db-prod) product/coll-path cids))

  (def info (kotori-info "0009"))
  (def products (select-scheduled-products
                 {:db          (db-prod)
                  :info        info
                  :genre-id    4024
                  :creds       (creds)
                  :limit       100
                  :screen-name (:screen-name info)}))
  (count products)

  (def product (first products))
  (def next (lib/->next (first products)))
  )
