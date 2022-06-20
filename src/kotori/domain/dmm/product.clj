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

(def doujin-coll-path (str dmm/doc-path "/doujins"))
(defn doujin-doc-path [cid] (str doujin-coll-path "/" cid))

(def girls-coll-path (str dmm/doc-path "/girls"))
(defn girls-doc-path [cid] (str girls-coll-path "/" cid))

;; TODO api->docの変換はdomainではなくlibに移動すべき
;; これは副作用のないデータの変換に過ぎないので.

(defn api->cid [resp]
  (:content_id resp))

(defn api->title [resp]
  (:title resp))

(defn api->url [resp]
  (:URL resp))

;; 普通の動画だと :affiliateURLspという属性があるがVR動画はない.
;; sp自体が古い仕様でこれから:affiliateURLに統一されることを予測して
;; spの属性をみるのはやめる. spはおそらく携帯用.
(defn api->affiliate-url [resp]
  (:affiliateURL resp))

(defn api->actresses [resp]
  (get-in resp [:iteminfo :actress]))

(defn api->released-time [resp]
  (let [date-str (:date resp)]
    (-> date-str
        (time/parse-dmm-timestamp)
        (time/->fs-timestamp))))

(defn api->genres [resp]
  (get-in resp [:iteminfo :genre]))

(defn api->genre-ids [resp]
  (->> (api->genres resp)
       (map #(get % "id"))
       (into [])))

(defn api->maker-id [resp]
  (-> resp
      (get-in [:iteminfo :maker])
      first
      :id))

(defn api->director-id [resp]
  (-> resp
      (get-in [:iteminfo :director])
      first
      :id))

(defn api->label-id [resp]
  (-> resp
      (get-in [:iteminfo :label])
      first
      :id))

(defn api->series-id [resp]
  (-> resp
      (get-in [:iteminfo :series])
      first
      :id))

(defn sample-movie? [resp]
  (contains? resp :sampleMovieURL))

(defn sample-image? [resp]
  (contains? resp :sampleImageURL))

(defn- info->id [info key]
  (-> info
      key
      first
      :id))

(defn assoc-iteminfo [data info]
  (cond-> data
    (:maker info)    (assoc :maker_id (info->id info :maker))
    (:label info)    (assoc :label_id (info->id info :label))
    (:director info) (assoc :director_id (info->id info :director))
    (:series info)   (assoc :series_id (info->id info :series))))

(defn api->doc
  "dmm response map -> firestore doc mapの写像"
  [resp]
  (let [actresses (api->actresses resp)
        iteminfo  (:iteminfo resp)
        data      {:cid             (api->cid resp)
                   :title           (api->title resp)
                   :url             (api->url resp)
                   :affiliate_url   (api->affiliate-url resp)
                   :actresses       actresses
                   :actress_count   (count actresses)
                   :released_time   (api->released-time resp)
                   :genres          (api->genres resp)
                   :no_sample_movie (not (sample-movie? resp))
                   :no_sample_image (not (sample-image? resp))}]
    (-> data
        (assoc-iteminfo iteminfo)
        (assoc :raw resp))))

;; docごとにtimestampを生成すると
;; ms単位の誤差によって正確なソートができないため
;; 予め生成した共通のtimestampを外部からもらって設定する.
(defn set-crawled-timestamp [timestamp data]
  (assoc data :last_crawled_time timestamp))

(defn set-scraped-timestamp [timestamp data]
  (assoc data :last_scraped_time timestamp))

(defn doc->genre-ids [doc]
  (->> (:genres doc)
       (map #(get % "id"))
       (into [])))

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

(defn tweet->doc [tweet exinfo]
  (let [screen-name (tweet/->screen-name tweet)
        tweet-id    (tweet/->id tweet)
        tweet-time  (tweet/->created-time tweet)
        tweet-link  (tweet/->url screen-name tweet-id)
        tweet-key   (fs/make-nested-key ["tweets"
                                         screen-name tweet-id])
        tweet-val   {"screen_name" screen-name
                     ;; "user_id"            user-id
                     "tweet_id"    tweet-id
                     "tweet_time"  tweet-time
                     "tweet_link"  tweet-link
                     "text"        (:text tweet)}]
    {"last_tweet_time" tweet-time
     "last_tweet_name" screen-name
     "last_tweet_id"   tweet-id
     tweet-key         (merge tweet-val exinfo)}))

