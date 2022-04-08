(ns kotori.domain.tweet.post
  (:require
   [kotori.domain.tweet.core :as tweet]))

(defn ->coll-path [user-id]
  (str "tweets/" user-id "/posts"))

(defn ->doc-path [user-id tweet-id]
  (str (->coll-path user-id) "/" tweet-id))

(defn- created-time [tweet]
  (tweet/parse-timestamp (:created_at tweet)))

(defn ->data [tweet]
  (let [created-time (created-time tweet)
        user         (:user tweet)]
    {:tweet_id   (:id_str tweet)
     :user_id    (:id_str user)
     :text       (:text tweet)
     :created_at created-time
     :updated_at created-time}))
