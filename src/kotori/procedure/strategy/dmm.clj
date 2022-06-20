(ns kotori.procedure.strategy.dmm
  "商品選択戦略"
  (:require
   [clojure.string :as str]
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.genre.anime :as anime]
   [kotori.domain.dmm.genre.core :as genre]
   [kotori.domain.dmm.genre.videoa :as videoa]
   [kotori.domain.dmm.genre.videoc :as videoc]
   [kotori.domain.dmm.product :as product]
   [kotori.domain.kotori.core :refer [guest-user]]
   [kotori.lib.firestore :as fs]
   [kotori.lib.kotori :as lib]
   [kotori.lib.provider.dmm.product :as lib-dmm]
   [kotori.lib.time :as time])
  (:import
   (kotori.domain.kotori.core
    Kotori)))

(defn ->genre-ids [product]
  (->> product
       :genres
       (map #(get % "id"))
       (into [])))

(defn contains-genre? [genre-id-set product]
  (some true?
        (map #(contains? genre-id-set %) (->genre-ids product))))

(defn ->st-include [genre-ids]
  (filter #(contains-genre? genre-ids %)))

(defn ->st-exclude [genre-ids]
  (remove #(contains-genre? genre-ids %)))

(defn no-genres? [product]
  (nil? (:genres product)))

(def st-exclude-no-genres
  (remove #(no-genres? %)))

(defn make-st-exclude-ng-genres [ids]
  (remove
   #(some true? (map
                 (fn [genre]
                   (let [id (get genre "id")]
                     (contains? ids id)))
                 (:genres %)))))

(def st-exclude-ng-genres
  (make-st-exclude-ng-genres videoa/ng-genres))

(def st-exclude-ng-genres-non-hard
  (make-st-exclude-ng-genres videoa/ng-genres-non-hard))

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

(def st-include-vr
  (filter #(contains-genre? videoa/vr-ids %)))

(def st-exclude-vr
  (remove #(contains-genre? videoa/vr-ids %)))

;; さもあり監督はハズレなし.
(def director-id-samoari 114124)

(defn contains-chikubi? [p]
  (or
   (= (:director-id p) director-id-samoari)
   (str/includes? (:title p) "乳首")
   (str/includes? (:description p) "乳首")
   (str/includes? (:title p) "チクビ")))

(def st-include-chikubi
  (filter contains-chikubi?))

(def st-exclude-chikubi
  (remove contains-chikubi?))

;; かつお物産は人気なもののちょっとジャンルからそれるので排他しておく.
(def maker-id-katsuo 6608)
(def st-exclude-katsuo
  (remove (fn [p] (= (:maker-id p) maker-id-katsuo))))

(def maker-id-gas 45050)
(def st-exclude-gas
  (remove (fn [p] (= (:maker-id p) maker-id-gas))))

(def st-include-videoc-fat
  (filter #(contains-genre? videoc/fat-ids %)))

(def st-exclude-videoc-fat
  (remove #(contains-genre? videoc/fat-ids %)))

(defn no-actress? [product]
  (let [count (:actress-count product)]
    (or (nil? count) (zero? count))))

(def st-exclude-no-actress
  (remove #(no-actress? %)))

(defn released? [product]
  (let [released-time (:released-time product)]
    ;; released-timeが存在しない商品がある？
    ;; ぬるぽしたのでガードを入れておく. 気が向いたら調査.
    (and released-time (time/past-now? released-time))))

(def st-exclude-not-yet-released
  (filter #(released? %)))

(def st-exclude-amateur
  (remove #(or (no-actress? %)
               (contains-genre? videoa/amateur-ids %))))

(def st-include-amateur
  (filter #(or (no-actress? %)
               (contains-genre? videoa/amateur-ids %))))

(def st-exclude-omnibus
  (remove #(> (:actress-count %) 4)))

;; アニメ: ルネサスピクチャーズ
(def st-include-lunesoft
  (filter #(= (:maker-id %) 45012)))

(def videoa-default-xst
  [st-exclude-ng-genres  ; NGジャンル除外
   st-exclude-no-samples ; サンプル画像と動画なしを除外
   ])

(def videoa-actress-xst
  [st-exclude-no-actress ;; 女優数0を除外
   st-exclude-omnibus ;; 詰め合わせを除外
   ])

(def videoa-extra-xst
  [st-exclude-amateur
   st-exclude-vr])

(defmulti make-strategy :code)

(defmethod make-strategy "0001" [_]
  (concat videoa-default-xst
          videoa-actress-xst
          videoa-extra-xst))

(defmethod make-strategy "0002" [_]
  (concat videoa-default-xst
          [st-exclude-vr
           st-include-chikubi
           st-exclude-katsuo
           st-exclude-gas]))

(defmethod make-strategy "0009" [_]
  (conj videoa-default-xst
        st-exclude-vr))

(defmethod make-strategy "0020" [_]
  [st-exclude-ng-genres-non-hard
   st-exclude-vr
   st-exclude-no-samples])

(defmethod make-strategy "0025" [_]
  (concat videoa-default-xst
          [st-exclude-vr
           st-exclude-chikubi]))

;; 現状公式にはルネサスピクチャーズのみ動画サンプルの利用が可能なので
;; ほかのメーカーを投稿しないように抑止をいれておく.
;; いちおうほかの動画もAPIで取得できちゃうので
;; なにが許可されているのか判定しづらい.
;; 現在はだんだんサンプル動画が許可され始めているので様子をみて素材開放を待つ
(defmethod make-strategy "0024" [_]
  [st-include-lunesoft
   (make-st-exclude-ng-genres anime/ng-genres)
   st-exclude-no-samples])

(defmethod make-strategy "0027" [_]
  [(make-st-exclude-ng-genres videoc/ng-genres)
   st-exclude-no-samples
   st-exclude-videoc-fat])

(defmethod make-strategy "0028" [_]
  [st-exclude-ng-genres
   st-exclude-movie
   st-exclude-no-image
   st-exclude-no-actress
   st-exclude-omnibus])

(defmethod make-strategy "0040" [_]
  [(make-st-exclude-ng-genres videoc/ng-genres)
   st-exclude-no-samples
   st-include-videoc-fat])

(defmethod make-strategy :default [_]
  (concat videoa-default-xst
          videoa-extra-xst))

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

(defn get-last-crawled-time [db floor genre-id]
  (-> db
      (fs/get-in dmm/doc-path :last-crawled-time)
      (get-in [floor "genres" (str genre-id) "timestamp"])))

(defn assoc-last-crawled-time [m db floor genre-id]
  (let [last-crawled-time (get-last-crawled-time db floor genre-id)]
    (assoc m :last-crawled-time last-crawled-time)))

(defn select-scheduled-products-with-xst
  [{:keys [db past-days] :or {past-days 28}} xst coll-path doc-ids]
  (let [st-exclude-recently-tweeted
        (make-st-exclude-recently-tweeted past-days)
        products  (fs/get-docs-by-ids db coll-path doc-ids)
        xstrategy (apply comp
                         st-skip-not-yet-crawled
                         st-skip-debug
                         st-skip-ignore
                         st-exclude-not-yet-released
                         st-exclude-recently-tweeted
                         xst)]
    (->> products
         (into [] xstrategy))))

(defn select-scheduled-products
  [{:keys [info db limit creds genre-id floor coll-path sort]
    :as   m
    :or   {limit    300
           genre-id (:genre-id info)
           sort     "rank"}}]
  (let [genre     (genre/make-genre floor)
        coll-path (or coll-path (genre/->coll-path genre))
        products  (lib-dmm/get-products {:floor    floor
                                         :genre-id genre-id
                                         :creds    creds
                                         :limit    limit
                                         :sort     sort})
        xst       (cond-> (make-strategy info)
                    ;; FIXME crawlとscrapingがまだの場合の検討
                    (not= floor "anime")
                    (conj st-skip-not-yet-scraped))
        doc-ids   (map :content_id products)
        products  (select-scheduled-products-with-xst
                   m xst coll-path doc-ids)]
    (if (and (zero? (count products)) (= sort "rank"))
      (select-scheduled-products (assoc m :sort "review"))
      (take limit products))))

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
  )

(comment
  ;;;;;;;;;;;
  (def info (kotori-info "0002"))
  (def products
    (into []
          (select-scheduled-products
           {:db          (db-prod)
            :creds       (creds)
            :info        info
            :limit       300
            ;; :sort        "review"
            :screen-name (:screen-name info)})))
  (count products)
  ;;
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

(comment
  (fs/get-docs (db-prod) "providers/dmm/amateurs"
               (fs/query-filter
                "last_crawled_time"
                (get-last-crawled-time (db-prod) "videoc" "default")))

  (def ts (get-last-crawled-time (db-prod) "videoc" "default"))

  (require '[firestore-clj.core :as f])

  (-> (db-prod)
      (f/coll "providers/dmm/amateurs")
      (f/filter= "last_crawled_time" ts)
      f/pullv)

  )

(comment
  (defprotocol Strategy
    (make-strategy [this]))

  (extend-protocol Strategy
    Kotori
    (make-strategy [this]
      [st-exclude-ng-genres
       st-exclude-no-samples
       st-exclude-vr
       st-exclude-amateur
       st-exclude-omnibus]))
  )

(comment

  (def info (kotori-info "0002"))
  (def genre-id (:genre-id info))

  (def products (lib-dmm/get-products {:genre-id genre-id
                                       :creds    (creds)
                                       :limit    200
                                       :sort     "review"}))
  (count products)

  (def xst (make-strategy info))
  (def doc-ids  (map :content_id products))

  (def products  (fs/get-docs-by-ids (db-prod)
                                     product/coll-path
                                     doc-ids))

  (def xstrategy (apply comp
                        st-skip-not-yet-crawled
                        st-skip-not-yet-scraped
                        st-skip-debug
                        st-skip-ignore
                        xst
                        ))

  (def ret (into [] xstrategy products))

  )
