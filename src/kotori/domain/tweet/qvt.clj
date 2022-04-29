(ns kotori.domain.tweet.qvt
  (:require
   [kotori.domain.source.core :as core]
   [kotori.domain.tweet.core :as tweet]
   [kotori.lib.firestore :as fs]
   [kotori.lib.kotori :as lib]))

(def default "qvt_0001")

(defn get-source
  "与えられたラベル名に対応するソースを返す.
  ラベル名が未知のときはdefault(qvt_0001)を返す."
  [label]
  (or
   (core/->source label)
   (core/->source default)))

(defn build-text [qvt type data]
  (let [url     (:url qvt)
        text    (:text data)
        default (str text "\n" url)
        title   (:title qvt)
        desc    (lib/desc->trimed (:description qvt))
        summary (:summary qvt)
        message (-> (cond
                      (= type "title")       title
                      (= type "description") (or desc title)
                      (= type "summary")     (or summary desc title)
                      :else                  nil)
                    (lib/ng->ok))]
    (if message
      (str message "\n\n" default)
      default)))

;; TODO とりあえずuser-idは必要なユースケースが現れたら対応.
;; それまえはコメントアウトしておく.
(defn ->doc [qvt tweet]
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

(defn doc-> [data]
  (let [cid         (:cid data)
        title       (:title data)
        tweet-id    (:last-tweet-id data)
        screen-name (:last-tweet-name data)
        tweet-time  (:last-tweet-time data)
        crawled?    (contains? data :last-crawled-time)
        summary     (:summary data)
        description (:description data)
        url         (tweet/->quoted-video-url screen-name tweet-id)]
    {:url         url
     :cid         cid
     :title       title
     :tweet-id    tweet-id
     :screen-name screen-name
     :tweet-time  tweet-time
     :summary     summary
     :description description
     :crawled?    crawled?}))

