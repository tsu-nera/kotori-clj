(ns kotori.domain.tweet.core
  (:require
   [kotori.lib.time :as t]))

(defn ->url
  "ツイートURL"
  [screen-name tweet-id]
  (str "https://twitter.com/" screen-name "/status/" tweet-id))

(defn ->user-home-url
  "ユーザホーム"
  [screen-name]
  (str "https://twitter.com/" screen-name))

(defn ->user-home-url-by-id
  "ユーザホームhttps://twitter.com/i/user/:user_id"
  [user-id]
  (str "https://twitter.com/i/user/" user-id))

(defn ->quoted-video-url
  "動画引用ツイート用URL"
  [screen-name tweet-id]
  (str (->url screen-name tweet-id) "/video/1"))

(def timestamp-format "EEE MMM dd HH:mm:ss Z yyyy")

(defn parse-timestamp
  "月と曜日が英語表記の場合のparseがうまくいかないので
  とりあえず実績のあるSimpleDateFormatで対処することにした."
  [^String timestamp]
  (t/parse-timestamp-sdf timestamp-format timestamp))

(defn ->id [tweet]
  (:id_str tweet))

(defn ->screen-name [tweet]
  (get-in tweet [:user :screen_name]))

(defn ->created-time [tweet]
  (parse-timestamp (:created_at tweet)))

(comment
  (def timestamp "Sat Mar 26 02:15:15 +0000 2022")
  (parse-timestamp timestamp)
  )
