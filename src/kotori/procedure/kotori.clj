(ns kotori.procedure.kotori
  (:require
   [kotori.domain.dmm.product :as product]
   [kotori.domain.kotori :as d]
   [kotori.domain.source.meigen :as meigen]
   [kotori.domain.tweet.core :as tweet]
   [kotori.domain.tweet.post :as post]
   [kotori.domain.tweet.qvt :as qvt]
   [kotori.lib.discord :as discord]
   [kotori.lib.firestore :as fs]
   [kotori.procedure.strategy :as st]
   [twitter-clj.private :as private]))

(defn make-info [{:keys [screen-name user-id auth-token ct0 proxy]}]
  (let [creds   (d/->Creds auth-token ct0)
        proxies (d/map->Proxies proxy)]
    (d/->Info screen-name user-id creds proxies)))

(defn tweet [{:keys [^d/Info info db text type]}]
  (let [{:keys [user-id creds proxies]}
        info
        result   (private/create-tweet creds proxies text)
        tweet-id (:id_str result)
        doc-path (post/->doc-path user-id tweet-id)
        data     (post/->data result type)]
    (try
      (println (str "post tweet completed. id=" tweet-id))
      (fs/set! db doc-path data)
      result
      (catch Exception e
        (println "post tweet Failed." (.getMessage e))))))

(defn tweet-random [{:as params}]
  (let [text (meigen/make-tweet-text)]
    (tweet (assoc params :text text :type :text))))

(defn qvt->discord! [qvt tweet]
  (let [screen-name        (tweet/->screen-name tweet)
        tweet-id           (tweet/->id tweet)
        tweet-link         (tweet/->url screen-name tweet-id)
        quoted-screen-name (:screen-name qvt)
        quoted-tweet-id    (:tweet-id qvt)
        quoted-link        (tweet/->url
                            quoted-screen-name quoted-tweet-id)
        message            (str screen-name " post qvt completed.\n"
                                tweet-link "\n"
                                quoted-link "\n")]
    (discord/notify! :kotori-qvt message)))

(defn tweet-quoted-video
  "動画引用ツイート"
  [{:keys [^d/Info info db] :as params}]
  (let [screen-name (:screen-name info)
        qvt         (st/select-next-qvt-product
                     {:db db :screen-name screen-name})
        cid         (:cid qvt)
        text        (qvt/->tweet-text qvt)
        doc-path    (product/doc-path cid)
        result      (tweet (assoc params :text text :type :qvt))
        qvt-data    (qvt/->data qvt result)]
    (do
      ;; dmm/products/{cid} の情報を更新
      (fs/update! db doc-path qvt-data)
      (qvt->discord! qvt result))
    result))

(defn tweet-morning
  [{:as params}]
  (tweet (assoc params :text "おはようございます" :type :text)))

(defn tweet-evening
  [{:as params}]
  (tweet (assoc params :text "今日もお疲れ様でした" :type :text)))

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
  (tweet-random params)

  ;;;;;;;;;;;;;
  (def result (tweet-quoted-video params))

  (def qvt (st/select-next-qvt-product {:db (db)}))
  (def qvt-data (qvt/->data qvt result))
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
