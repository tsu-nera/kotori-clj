(ns kotori.domain.tweet.qvt
  (:require
   [kotori.domain.source.core :as core]
   [kotori.domain.tweet.core :as tweet]
   [kotori.lib.firestore :as fs]))

(def default "qvt_0001")

(defn get-source
  "与えられたラベル名に対応するソースを返す.
  ラベル名が未知のときはdefault(qvt_0001)を返す."
  [label]
  (or
   (core/->source label)
   (core/->source default)))

(defn build-text [qvt data]
  (let [url  (:url qvt)
        text (:text data)]
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

;;;;;;;;;;;;;;;;;

(comment

  (core/->source default)

  )
