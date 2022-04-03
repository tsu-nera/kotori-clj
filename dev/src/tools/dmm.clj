(ns tools.dmm
  (:require
   [devtools :refer [env]]
   [firebase :refer [db-dev db-prod]]
   [firestore-clj.core :as f]
   [kotori.lib.firestore :as fs]
   [kotori.lib.json :as json]
   [kotori.procedure.dmm :as dmm]
   [kotori.procedure.tweet.post :as post]))

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

(defn ->tweet [screen-name data]
  {(str "tweets." screen-name) data
   "last_tweet_name"           screen-name
   "last_tweet_time"           (:tweet-time data)})

(defn ->path [data]
  (str "providers/dmm/products/" (:cid data)))

(defn assoc-post [db screen-name post]
  (->> post
       (make-dmm-tweet)
       ((juxt ->path
              #(->tweet screen-name %)))
       ((fn [[path tweet]]
          (fs/update! db path tweet)))))

(defn assoc-post [db screen-name post]
  (->> post
       (make-dmm-tweet)
       ((juxt ->path
              #(->tweet screen-name %)))
       ((fn [[path tweet]]
          (fs/update! db path tweet)))))

(defn assoc-posts [db screen-name posts]
  (->> posts
       (map make-dmm-tweet)
       (map (juxt ->path
                  #(->tweet screen-name %)))
       (map (fn [[path tweet]]
              (fs/update! db path tweet)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;;;
  (require '[firebase :refer [db-dev db-prod]])

  (def screen-name "")
  (def user-id "")

  (def resp (post/get-video-posts {:db      (db-prod)
                                   :user-id user-id
                                   :weeks   1}))
  (count resp)
  ;;;;;;;;;;;;;;;

  (def post (first resp))
  (assoc-post (db-dev) screen-name post)

  ;;;;;;;;;;;;;;;;;;;;;;

  (def posts (take 3 resp))
  (assoc-posts (db-dev) screen-name posts)

  ;;;;

  (require '[kotori.procedure.dmm :as dmm])
  (def product (dmm/crawl-product! {:db (db-dev) :env (env) :cid "waaa00067"}))

  ;;;
  )
