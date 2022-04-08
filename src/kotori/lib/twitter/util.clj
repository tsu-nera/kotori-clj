(ns kotori.lib.twitter.util)

(defn ->quoted-video-url
  "動画引用ツイート用URL"
  [screen-name tweet-id]
  (str "https://twitter.com/" screen-name
       "/status/" tweet-id "/video/1"))
