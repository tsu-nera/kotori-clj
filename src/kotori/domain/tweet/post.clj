(ns kotori.domain.tweet.post
  (:require
   [kotori.domain.tweet.core :as tweet]
   [kotori.lib.time :as time]))

;; TODO 見直し必要
(def data-type
  {:text  "text"
   :video "video"
   :photo "photo"
   :qvt   "quoted_video"})

(defn ->archive-data [data]
  (let [ts (time/fs-now)]
    (assoc data "deleted_at" ts)))

(defn ->doc
  ([tweet]
   (let [created-time (tweet/->created-time tweet)
         screen-name  (tweet/->screen-name tweet)
         tweet-id     (tweet/->id tweet)
         url          (tweet/->url screen-name tweet-id)
         user-id      (tweet/->user-id tweet)]
     {:user_id             user-id
      :screen_name         screen-name
      :tweet_id            tweet-id
      :tweet_link          url
      :text                (:text tweet)
      :created_at          created-time
      :updated_at          created-time
      :like_count          0
      :retweet_count       0
      :self_retweet        false
      :self_retweet_count  0
      :other_retweet       false
      :other_retweet_count 0}))
  ([type tweet]
   (-> tweet
       ->doc
       (cond-> type
         (assoc :type (type data-type))))))

(comment
;;;
  ;; recordとmultimethodの技術検証
  (defrecord QuotedVideo
      [last-quoted-time
       last-quoted-name
       quoted-tweets])

  (defmulti ->ex-data (fn [ex tweet] (type ex)))
  (defmethod ->ex-data QuotedVideo [ex tweet] tweet)

  (def info (map->QuotedVideo {:last-quoted-name "test"}))
  (->ex-data info {})
;;;
  )
