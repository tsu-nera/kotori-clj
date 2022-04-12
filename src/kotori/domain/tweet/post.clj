(ns kotori.domain.tweet.post
  (:require
   [kotori.domain.tweet.core :as tweet]))

(defn ->coll-path [user-id]
  (str "tweets/" user-id "/posts"))

(defn ->doc-path [user-id tweet-id]
  (str (->coll-path user-id) "/" tweet-id))

(defn ->data [tweet]
  (let [created-time (tweet/->created-time tweet)
        tweet-id     (tweet/->id tweet)
        user         (:user tweet)]
    {:tweet_id   tweet-id
     :user_id    (:id_str user)
     :text       (:text tweet)
     :created_at created-time
     :updated_at created-time}))

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
