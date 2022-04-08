(ns kotori.domain.tweet.post
  (:require
   [kotori.lib.time :as time]))

(defn ->coll-path [user-id]
  (str "tweets/" user-id "/posts"))
(defn ->doc-path [user-id tweet-id]
  (str (->coll-path user-id) "/" tweet-id))

(defn ->data [tweet]
  (let [created_at (time/parse-twitter-timestamp (:created_at tweet))
        user       (:user tweet)]
    {"tweet_id"   (:id_str tweet)
     "user_id"    (:id_str user)
     "text"       (:text tweet)
     "created_at" created_at
     "updated_at" created_at}))
