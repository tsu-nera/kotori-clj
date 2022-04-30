(ns tools.suspened
  "凍結アカウント後処理 aka. 凍らされた小鳥への鎮魂歌(レクイエム)"
  (:require
   [devtools :refer [->screen-name ->user-id]]
   [firebase :refer [db-prod]]
   [firestore-clj.core :as f]))

(def dead-name (->screen-name "0005"))

(defn ->retweets-coll-path [code]
  (let [user-id (->user-id code)]
    (str "tweets/" user-id "/retweets")))

(defn delete-suspended-retweets! [db from-code to-code]
  (let [dead-name  (->screen-name to-code)
        alive-path (->retweets-coll-path from-code)]
    (f/delete-all!
     (-> (f/coll db alive-path)
         (f/filter= "screen_name" dead-name)))))

#_(delete-suspended-retweets! (db-prod) "0012" "0005")
