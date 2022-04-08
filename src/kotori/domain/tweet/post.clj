(ns kotori.domain.tweet.post)

(defn ->coll-path [user-id]
  (str "tweets/" user-id "/posts"))
(defn ->doc-path [user-id tweet-id]
  (str (->coll-path user-id) "/" tweet-id))
