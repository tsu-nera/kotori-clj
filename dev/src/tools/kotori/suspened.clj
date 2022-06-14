(ns tools.kotori.suspened
  "凍結アカウント後処理 aka. 凍らされた小鳥への鎮魂歌(レクイエム)"
  (:require
   [devtools :refer [->screen-name ->user-id kotori-params]]
   [firebase :refer [db-prod]]
   [firestore-clj.core :as f]
   [kotori.domain.dmm.product :as product]
   [kotori.lib.firestore :as fs]
   [kotori.procedure.kotori.core :as kotori]))

(def dead-name (->screen-name "0012"))

(defn ->retweets-coll-path [code]
  (let [user-id (->user-id code)]
    (str "tweets/" user-id "/retweets")))

(defn delete-suspended-retweets! [db from-code]
  (let [alive-path (->retweets-coll-path from-code)]
    (f/delete-all!
     (-> (f/coll db alive-path)
         (f/filter= "screen_name" dead-name)))))

;;;;
(comment
  (def retweeters
    ["0002" "0007" "0009" "0010"
     "0011" "0019" "0020" "0025"])

  (let [db (db-prod)]
    (for [code retweeters]
      (delete-suspended-retweets! db code)))

  #_(delete-suspended-retweets! (db-prod) "0001")
  )
;;;;;;;

(defn get-quoted-products [db quoted-screen-name]
  (fs/get-filter-docs db product/coll-path
                      {"last_tweet_name"  dead-name
                       "last_quoted_name" quoted-screen-name}))

(defn resolve-qvt [db code]
  (let [screen-name (->screen-name code)]
    (when-let [products (get-quoted-products db screen-name)]
      (->> products
           (map (fn [m]
                  (let [tweet-id (:last-quoted-tweet-id m)]
                    {:screen-name screen-name
                     :tweet-id    tweet-id})))))))

(defn delete-qvt-tweet! [db code tweet-id]
  (let [params (kotori-params db code)]
    (kotori/delete-tweet!
     (assoc params :tweet-id tweet-id))))

(defn delete-qvt-tweets! [db code]
  (let [tweet-ids
        (into [] (map :tweet-id (resolve-qvt db code)))]
    (doseq [tweet-id tweet-ids]
      (delete-qvt-tweet! db code tweet-id)
      (Thread/sleep 3000))))

(comment
  (->user-id "0021")
  (delete-qvt-tweet! (db-prod) "0019" "xxx")

  (->screen-name "0021")
  (resolve-qvt (db-prod) "0021")
  (delete-qvt-tweets! (db-prod) "0025")
  )

;;;;;;;;;;;;;;;

(defn remove-last-qvt-product! [db cid]
  (-> db
      (f/doc (product/doc-path cid))
      (f/dissoc! "last_tweet_id"
                 "last_tweet_name"
                 "last_tweet_time"
                 "last_quoted_name"
                 "last_quoted_time"
                 "last_quoted_tweet_id"
                 (str "tweets." dead-name)
                 "quoted_tweets")))

(defn remove-last-qvt-products! [db]
  (let [products (fs/get-filter-docs
                  db product/coll-path
                  {"last_tweet_name" dead-name})
        cids     (map :cid products)]
    (doseq [cid cids]
      (remove-last-qvt-product! db cid))))

(comment
  (remove-last-qvt-products! (db-prod))
  )
