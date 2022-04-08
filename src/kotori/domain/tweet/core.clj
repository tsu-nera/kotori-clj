(ns kotori.domain.tweet.core
  (:require
   [kotori.lib.time :as t]))

(defn ->quoted-video-url
  "動画引用ツイート用URL"
  [screen-name tweet-id]
  (str "https://twitter.com/" screen-name
       "/status/" tweet-id "/video/1"))

(def timestamp-format "EEE MMM dd HH:mm:ss Z yyyy")

(defn parse-timestamp
  "月と曜日が英語表記の場合のparseがうまくいかないので
  とりあえず実績のあるSimpleDateFormatで対処することにした."
  [^String timestamp]
  (t/parse-timestamp-sdf timestamp-format timestamp))

(comment
  (def timestamp "Sat Mar 26 02:15:15 +0000 2022")
  (parse-timestamp timestamp)
  )
