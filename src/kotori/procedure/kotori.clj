(ns kotori.procedure.kotori
  (:require
   [kotori.domain.dmm.product :as product]
   [kotori.domain.kotori :as d]
   [kotori.domain.source.meigen :refer [meigens]]
   [kotori.domain.tweet.core :as tweet]
   [kotori.domain.tweet.post :as post]
   [kotori.lib.firestore :as fs]
   [kotori.procedure.strategy :as st]
   [twitter-clj.private :as private]))

(defn pick-random []
  (rand-nth meigens))

(defn make-text [data]
  (let [{content :content, author :author} data]
    (str content "\n\n" author)))

(defn make-info [{:keys [screen-name user-id auth-token ct0 proxy]}]
  (let [creds   (d/->Creds auth-token ct0)
        proxies (d/map->Proxies proxy)]
    (d/->Info screen-name user-id creds proxies)))

(defn tweet [{:keys [^d/Info info db text]}]
  (let [{:keys [user-id creds proxies]}
        info
        result   (private/create-tweet creds proxies text)
        tweet-id (:id_str result)
        doc-path (post/->doc-path user-id tweet-id)
        data     (post/->data result)]
    (try
      (println (str "post tweet completed. id=" tweet-id))
      (fs/set! db doc-path data)
      result
      (catch Exception e
        (println "post tweet Failed." (.getMessage e))))))

(defn tweet-random [{:as params}]
  (let [data (pick-random)
        text (make-text data)]
    (tweet (assoc params :text text))))

(defn ->qvt-text [qvt]
  (let [url     (:url qvt)
        message "やべーよ!"]
    (str message "\n" url)))

;; TODO とりあえずuser-idは必要なユースケースが現れたら対応.
;; それまえはコメントアウトしておく.
(defn make-qvt-data [qvt tweet]
  (let [screen-name      (get-in tweet [:user :screen_name])
        tweet-id         (tweet/->id tweet)
        tweet-time       (tweet/->created-time tweet)
        tweet_link       (tweet/->url screen-name tweet-id)
        quoted-tweet-key (fs/make-nested-key ["quoted_tweets"
                                              screen-name tweet-id])
        quoted-tweet-val {"screen_name"        screen-name
                          ;; "user_id"            user-id
                          "tweet_id"           tweet-id
                          "tweet_time"         tweet-time
                          "tweet_link"         tweet_link
                          "text"               (:text tweet)
                          "cid"                (:cid qvt)
                          "quoted_tweet_id"    (:tweet-id qvt)
                          ;; "quoted_user_id"     (:user-id qvt)
                          "quoted_screen_name" (:screen-name qvt)}]
    {"last_quoted_time"     tweet-time
     "last_quoted_name"     screen-name
     "last_quoted_tweet_id" tweet-id
     quoted-tweet-key       quoted-tweet-val}))

(defn tweet-quoted-video
  "動画引用ツイート"
  [{:keys [^d/Info info db] :as params}]
  (let [screen-name (:screen-name info)
        qvt         (st/select-next-qvt-product
                     {:db db :screen-name screen-name})
        cid         (:cid qvt)
        text        (->qvt-text qvt)
        doc-path    (product/doc-path cid)
        result      (tweet (assoc params :text text))
        qvt-data    (make-qvt-data qvt result)]
    ;; dmm/products/{cid} の情報を更新
    (fs/update! db doc-path qvt-data)
    result))

(defn tweet-morning
  [{:as params}]
  (tweet (assoc params :text "おはようございます")))

(defn tweet-evening
  [{:as params}]
  (tweet (assoc params :text "今日もお疲れ様でした")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn dummy [{:keys [^d/Info info db text]}]
  (assoc info :text text))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;;;
  (require '[firebase :refer [db]]
           '[devtools :refer [kotori-info]])

  (def params {:db (db) :info (kotori-info "0003")})

  ;;;;;;;;;;;;;
  (tweet-morning params)
  (tweet-evening params)

  (def text (make-text (pick-random)))
  (tweet-random params)

  ;;;;;;;;;;;;;
  (def result (tweet-quoted-video params))

  (def qvt (st/select-next-qvt-product {:db (db)}))
  (def qvt-data (make-qvt-data qvt result))
 ;;;
  )

(comment
  ;;;
  (require '[devtools :refer [twitter-auth]])
  (def auth (twitter-auth))

  (def tweet (private/get-tweet (twitter-auth) "1500694005259980800"))
  (def user (private/get-user (twitter-auth) "46130870"))
  (def resp (private/create-tweet (twitter-auth) "test"))

  (def status-id (:id_str resp))
  (def resp (private/delete-tweet (twitter-auth) status-id))
  ;;;
  )
