(ns tools.dmm
  (:require
   [devtools :refer [env]]
   [firebase :refer [db-dev db-prod]]
   [kotori.lib.firestore :as fs]
   [kotori.procedure.dmm :as dmm]
   [kotori.procedure.tweet.post :as post]))

(def dmm-coll-path "providers/dmm/products")

(defn make-dmm-tweet [screen-name post]
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
  (let [tweet-id   (:tweet-id data)
        tweet-time (:tweet-time data)]
    {(str "tweets" "." screen-name "." tweet-id) data
     "last_tweet_name"                           screen-name
     "last_tweet_time"                           tweet-time
     "last_tweet_id"                             tweet-id}))

(defn ->path [data]
  (str dmm-coll-path "/" (:cid data)))

(defn- update-with-recovery! [db path post]
  (if (fs/doc-exists? (db-dev) path)
    (fs/update! db path post)
    (doto db
      (fs/set!
       path
       (select-keys post
                    ["last_tweet_name"
                     "last_tweet_time"
                     "last_tweet_id"]))
      (fs/update!
       path
       (dissoc post
               "last_tweet_name" "last_tweet_time" "last_tweet_id")))))

(defn assoc-post [db screen-name post]
  (->> post
       (make-dmm-tweet screen-name)
       ((juxt ->path
              #(->tweet screen-name %)))
       ((fn [[path tweet]]
          (update-with-recovery! db path tweet)))))

(defn assoc-posts [db screen-name posts]
  (->> posts
       (map #(make-dmm-tweet screen-name %))
       (map (juxt ->path
                  #(->tweet screen-name %)))
       (map (fn [[path tweet]]
              (update-with-recovery! db path tweet)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment  ;;;
  (require '[firebase :refer [db-dev db-prod]])


  (def resp (post/get-video-posts {:db          (db-prod)
                                   :user-id     user-id
                                   :since-weeks 4
                                   :days        14}))
  (count resp)
  (assoc-posts (db-dev) screen-name resp)



  ;;;;;;;;;;;;;;;
  (def post (first resp))
  (assoc-post (db-dev) screen-name post)
  ;;;;;;;;;;;;;;;;;;;;;;

  (require '[kotori.procedure.dmm :as dmm])
  (def product (dmm/crawl-product!
                {:db (db-dev) :env (env) :cid "cjod00289"}))
 ;;;
  )
