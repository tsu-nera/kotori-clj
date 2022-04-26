(ns kotori.domain.dmm.product
  (:require
   [clojure.string :as string]
   [kotori.domain.dmm.core :as dmm]
   [kotori.lib.time :as time]))

(def coll-path (str dmm/doc-path "/products"))
(defn doc-path [cid] (str coll-path "/" cid))

(defn- ->cid [raw]
  (:content_id raw))

(defn- ->title [raw]
  (:title raw))

(defn- ->url [raw]
  (:URL raw))

(defn- ->affiliate-url [raw]
  (:affiliateURL raw))

(defn- ->actresses [raw]
  (get-in raw [:iteminfo :actress]))

(defn- ->performer [raw]
  (string/join
   "," (map #(:name %) (->actresses raw))))

(defn- ->released-time [raw]
  (let [date-str (:date raw)]
    (-> date-str
        (time/parse-dmm-timestamp)
        (time/->fs-timestamp))))

(defn- ->genres [raw]
  (get-in raw [:iteminfo :genre]))

(defn- ->genre [raw]
  (string/join
   "," (map #(:name %) (->genres raw))))

(defn- ->timestamp
  ([] (time/fs-now))
  ([_] (time/fs-now)))

(defn- sample-movie? [raw]
  (contains? raw :sampleMovieURL))

(defn- sample-image? [raw]
  (contains? raw :sampleImageURL))

(defn ->legacy [raw]
  {:cid         (->cid raw)
   :title       (->title raw)
   :url         (->affiliate-url raw)
   :performer   (->performer raw)
   :released_at (->released-time raw)
   :created_at  (->timestamp raw)
   :updated_at  (->timestamp raw)
   :genre       (->genre raw)
   :status      "READY"
   :ranking     nil
   :posted_at   nil
   :tweet_link  nil
   :tweet_id    nil})

(defn ->data
  "dmm response map -> firestore doc mapの写像"
  [raw]
  (let [actresses (->actresses raw)
        data      {:cid             (->cid raw)
                   :title           (->title raw)
                   :url             (->url raw)
                   :affiliate_url   (->affiliate-url raw)
                   :actresses       actresses
                   :actress_count   (count actresses)
                   :released_time   (->released-time raw)
                   :genres          (->genres raw)
                   :no_sample_movie (not (sample-movie? raw))
                   :no_sample_image (not (sample-image? raw))}
        legacy    (->legacy raw)]
    (-> data
        (assoc :raw raw)
        (assoc :legacy legacy))))

;; 最新人気ランキングを設定
;; 他にも価格, レビュー, マッチングのランキングがある.
;; ユースケースが明らかになったら改造する.
;; map-indexedとともに呼ばれることを想定.
;; firestoreの検索を想定してarrayではなくフィールドに保持する.
(defn set-rank-popular [i data]
  (let [ranking (+ i 1)]
    (-> data
        (assoc :rank_popular ranking)
        (assoc-in [:legacy :ranking] ranking))))

;; docごとにtimestampを生成すると
;; ms単位の誤差によって正確なソートができないため
;; 予め生成した共通のtimestampを外部からもらって設定する.
(defn set-crawled-timestamp [timestamp data]
  (assoc data :last_crawled_time timestamp))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[firebase :refer [db]])
  (require '[devtools :refer [env]])

  (def raw
    (dmm/get-product {:cid "ssis00165" :env (env)}))
  (def product
    (dmm/get-product {:cid "jusd00912" :env (env)}))

  (contains? raw :sampleMovieURL)
  (contains? product :sampleImageURL)

  (def actresses (->actresses raw))
  (count actresses)
  (map #(:name %) actresses)

  (def data (->data raw))

  (->released-time raw)
  )

(comment
  (require '[portal.api :as p])
  (def d (p/open))
  (reset! d (->data (dmm/get-product {:cid "ssis00335" :env (env)})))
  @d
  )
