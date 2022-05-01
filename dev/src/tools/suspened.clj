(ns tools.suspened
  "凍結アカウント後処理 aka. 凍らされた小鳥への鎮魂歌(レクイエム)"
  (:require
   [devtools :refer [->screen-name ->user-id]]
   [firebase :refer [db-prod]]
   [firestore-clj.core :as f]
   [kotori.domain.dmm.product :as product]
   [kotori.lib.firestore :as fs]
   [kotori.procedure.kotori.core :as kotori]))

(def dead-name (->screen-name "0005"))

(defn ->retweets-coll-path [code]
  (let [user-id (->user-id code)]
    (str "tweets/" user-id "/retweets")))

(defn delete-suspended-retweets! [db from-code]
  (let [alive-path (->retweets-coll-path from-code)]
    (f/delete-all!
     (-> (f/coll db alive-path)
         (f/filter= "screen_name" dead-name)))))

#_(delete-suspended-retweets! (db-prod) "0012")

;;;;;;;

(def qvt-codes ["0019" "0020" "0021" "0022" "0023" "0025"])
(def qvt-names (map ->screen-name qvt-codes))

(defn get-quoted-info [db coll-path quoted-screen-name]
  (fs/get-filter-docs db coll-path
                      {"last_tweet_name"  dead-name
                       "last_quoted_name" quoted-screen-name}))

#_(def ret
    (get-quoted-info
     (db-prod) product/coll-path (->screen-name "0025")))

#_(map (fn [m]
         (let [screen-name (:last-quoted-name m)
               tweet-id    (:last-quoted-tweet-id m)]
           (str "env USE_PRODUCTION=true inv kotori-delete-tweet " tweet-id " -s " screen-name))) ret)

;;;;;;;
