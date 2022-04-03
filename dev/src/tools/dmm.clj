(ns tools.dmm
  (:require
   [devtools :refer [env]]
   [firebase :refer [db-dev db-prod]]
   [kotori.lib.firestore :as fs]
   [kotori.procedure.tweet.post :as post]))

#_(defn get-twitter-posts [])

(comment
  (require '[firebase :refer [db-dev db-prod]])

  (def screen-name "")
  (def user-id "")

  (def resp (post/get-video-posts {:db      (db-prod)
                                   :user-id user-id
                                   :weeks   1}))
  (count resp)

  (def post (second resp))

  (defn make-dmm-tweet [post]
    {:cid         (:cid post)
     :media-id    (:media-id post)
     :media-key   (:media-key post)
     :screen-name screen-name
     :text        (:text post)
     :tweet-id    (:tweet-id post)
     :tweet-link  (:tweet-link post)
     :tweet-time  (:created-at post)
     :user-id     (:user-id post)})

  (def data (make-dmm-tweet post))

  (def tweet-info {:last_tweet_name screen-name
                   :last_tweet_time (:tweet-time data)}
    )
  (def product-path (str "providers/dmm/products/" (:cid data)))

  (fs/merge! (db-dev) product-path
             {:last_tweet_name screen-name
              :last_tweet_time (:tweet-time data)})

  (fs/assoc! (db-dev) product-path
             (str "tweets." screen-name) data)
  (fs/merge! (db-dev) product-path tweet-info)

  ;;;;;;;;;;;
  )
