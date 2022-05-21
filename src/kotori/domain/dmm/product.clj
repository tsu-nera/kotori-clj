(ns kotori.domain.dmm.product
  (:require
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.tweet.core :as tweet]
   [kotori.lib.firestore :as fs]
   [kotori.lib.time :as time]))

(def coll-path (str dmm/doc-path "/products"))
(defn doc-path [cid] (str coll-path "/" cid))

(def vr-coll-path (str dmm/doc-path "/vrs"))
(defn vr-doc-path [cid] (str vr-coll-path "/" cid))

(def amateur-coll-path (str dmm/doc-path "/amateurs"))
(defn amateur-doc-path [cid] (str amateur-coll-path "/" cid))

(defn- ->cid [raw]
  (:content_id raw))

(defn- ->title [raw]
  (:title raw))

(defn- ->url [raw]
  (:URL raw))

;; 普通の動画だと :affiliateURLspという属性があるがVR動画はない.
;; sp自体が古い仕様でこれから:affiliateURLに統一されることを予測して
;; spの属性をみるのはやめる. spはおそらく携帯用.
(defn- ->affiliate-url [raw]
  (:affiliateURL raw))

(defn- ->actresses [raw]
  (get-in raw [:iteminfo :actress]))

(defn- ->released-time [raw]
  (let [date-str (:date raw)]
    (-> date-str
        (time/parse-dmm-timestamp)
        (time/->fs-timestamp))))

(defn- ->genres [raw]
  (get-in raw [:iteminfo :genre]))

(defn- sample-movie? [raw]
  (contains? raw :sampleMovieURL))

(defn- sample-image? [raw]
  (contains? raw :sampleImageURL))

(defn api->data
  "dmm response map -> firestore doc mapの写像"
  [raw]
  (let [actresses (->actresses raw)
        data      {:cid             (->cid raw)
                   :title           (->title raw)
                   :url             (->url raw)
                   :affiliate_url   (->affiliate-url raw)
                   :actresses       actresses
                   :actress_count   (count actresses)
                   :released_time   (->released-time raw)
                   :genres          (->genres raw)
                   :no_sample_movie (not (sample-movie? raw))
                   :no_sample_image (not (sample-image? raw))}]
    (-> data
        (assoc :raw raw))))

;; docごとにtimestampを生成すると
;; ms単位の誤差によって正確なソートができないため
;; 予め生成した共通のtimestampを外部からもらって設定する.
(defn set-crawled-timestamp [timestamp data]
  (assoc data :last_crawled_time timestamp))

(defn set-scraped-timestamp [timestamp data]
  (assoc data :last_scraped_time timestamp))

;; TODO とりあえずuser-idは必要なユースケースが現れたら対応.
;; それまえはコメントアウトしておく.
(defn qvt->doc [qvt tweet]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[devtools :refer [env]])

  (def raw
    (dmm/get-product {:cid "ssis00165" :env (env)}))
  (def product
    (dmm/get-product {:cid "jusd00912" :env (env)}))

  (contains? raw :sampleMovieURL)
  (contains? product :sampleImageURL)

  (def actresses (->actresses raw))
  (count actresses)
  (map #(:name %) actresses)

  (def data (api->data raw))

  (->released-time raw)
  )

(comment
  (require '[portal.api :as p])
  (def d (p/open))
  (reset! d (api->data (dmm/get-product {:cid "ssis00335" :env (env)})))
  @d
  )
