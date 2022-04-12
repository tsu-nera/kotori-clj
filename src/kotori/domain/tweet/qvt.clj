(ns kotori.domain.tweet.qvt
  (:require
   [kotori.domain.tweet.core :as tweet]
   [kotori.lib.firestore :as fs]))

(def source-text
  ["やべーよ!"
   "超やべーよ!"
   "まじやべーよ!"
   "くそやべーよ!"
   "ぱねーよ!"
   "まじぱねーよ!"])

(defn ->tweet-text [qvt]
  (let [url  (:url qvt)
        text (rand-nth source-text)]
    (str text "\n" url)))

;; TODO とりあえずuser-idは必要なユースケースが現れたら対応.
;; それまえはコメントアウトしておく.
(defn ->data [qvt tweet]
  (let [screen-name      (tweet/->screen-name tweet)
        tweet-id         (tweet/->id tweet)
        tweet-time       (tweet/->created-time tweet)
        tweet-link       (tweet/->url screen-name tweet-id)
        quoted-tweet-key (fs/make-nested-key ["quoted_tweets"
                                              screen-name tweet-id])
        quoted-tweet-val {"screen_name"        screen-name
                          ;; "user_id"            user-id
                          "tweet_id"           tweet-id
                          "tweet_time"         tweet-time
                          "tweet_link"         tweet-link
                          "text"               (:text tweet)
                          "cid"                (:cid qvt)
                          "quoted_tweet_id"    (:tweet-id qvt)
                          ;; "quoted_user_id"     (:user-id qvt)
                          "quoted_screen_name" (:screen-name qvt)}]
    {"last_quoted_time"     tweet-time
     "last_quoted_name"     screen-name
     "last_quoted_tweet_id" tweet-id
     quoted-tweet-key       quoted-tweet-val}))
