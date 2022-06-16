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
        desc    (some-> (:description qvt) lib/desc->trimed)
        ;; summary (:summary qvt)
        message (-> (cond
                      (= type "title")       title
                      (= type "description") (or desc title)
                      ;; (= type "summary")     (or summary desc title)
                      :else                  nil)
                    (lib/ng->ok))]
    (if message
      (str message "\n\n" default)
      default)))

(defn doc-> [data]
  (let [cid         (:cid data)
        title       (:title data)
        tweet-id    (:last-tweet-id data)
        screen-name (:last-tweet-name data)
        tweet-time  (:last-tweet-time data)
        crawled?    (contains? data :last-crawled-time)
        ;; summary     (:summary data)
        description (:description data)
        url         (tweet/->quoted-video-url screen-name tweet-id)]
    {:url         url
     :cid         cid
     :title       title
     :tweet-id    tweet-id
     :screen-name screen-name
     :tweet-time  tweet-time
     ;; :summary     summary
     :description description
     :crawled?    crawled?}))

(defn ->doc [qvt]
  (let [cid         (:cid qvt)
        screen-name (:screen-name qvt)
        tweet-id    (:tweet-id qvt)]
    {"cid"                  cid
     "last_quoted_name"     screen-name
     "last_quoted_tweet_id" tweet-id}))
