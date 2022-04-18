(ns kotori.procedure.kotori
  (:require
   [clojure.spec.alpha :as s]
   [kotori.domain.dmm.product :as product]
   [kotori.domain.kotori :as d]
   [kotori.domain.source.meigen :as meigen]
   [kotori.domain.tweet.core :as tweet]
   [kotori.domain.tweet.post :as post]
   [kotori.domain.tweet.qvt :as qvt]
   [kotori.lib.discord :as discord]
   [kotori.lib.firestore :as fs]
   [kotori.procedure.dmm :as dmm]
   [kotori.procedure.strategy :as st]
   [twitter-clj.private :as private]))

(defn make-info [{:keys [screen-name user-id auth-token ct0 proxy-map]}]
  (d/make-info screen-name user-id
               {:auth-token auth-token :ct0 ct0} proxy-map))

(defn tweet [{:keys [^d/Info info db text type]}]
  (let [{:keys [user-id cred proxy]}
        info
        result   (private/create-tweet cred proxy text)
        tweet-id (:id_str result)
        doc-path (post/->doc-path user-id tweet-id)
        data     (post/->data result type)]
    (try
      (println (str "post tweet completed. id=" tweet-id))
      (fs/set! db doc-path data)
      result
      (catch Exception e
        (println "post tweet Failed." (.getMessage e))))))

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

(defn select-next-qvt-product [{:as params}]
  (st/->next-qvt (first (st/select-tweeted-products params))))

(defn tweet-quoted-video
  "動画引用ツイート"
  [{:keys [^d/Info info db env] :as params}]
  (let [screen-name  (:screen-name info)
        qvt          (select-next-qvt-product
                      {:db db :screen-name screen-name})
        cid          (:cid qvt)
        text         (qvt/->tweet-text qvt)
        doc-path     (product/doc-path cid)
        crawled?     (:craweled? qvt)
        tweet-params (assoc params :text text :type :qvt)
        result       (tweet tweet-params)]
    (do
      ;; DMM商品情報 collectionを更新.
      (->> result
           (qvt/->data qvt)
           ;; dmm/products/{cid} の情報を更新
           (fs/update! db doc-path))
      ;; crawledされてない場合はここで追加で処理をする.
      ;; 通常はcrawledされているのでtoolで追加した場合がこうなる.
      ;; そんなに時間かからないと思うので同期処理
      (when-not crawled?
        (dmm/crawl-product! {:db db :env env :cid cid}))
      (qvt->discord! qvt result))
    result))

(defn tweet-morning
  [{:as params}]
  (tweet (assoc params :text "おはようございます" :type :text)))

(defn tweet-evening
  [{:as params}]
  (tweet (assoc params :text "今日もお疲れ様でした" :type :text)))

(defn tweet-random [{:as params}]
  (let [text (meigen/make-tweet-text)]
    (tweet (assoc params :text text :type :text))))

(defn tweet-source [{:keys [^d/Info info db env] :as params}]
  (let [text (meigen/make-tweet-text)]
    (tweet (assoc params :text text :type :text))))

(defn select-next-product [{:keys [db screen-name]}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (st/->next (first (st/select-scheduled-products
                     {:db db :screen-name screen-name}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn dummy [{:keys [^d/Info info db text]}]
  (assoc info :text text))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;;;
  (require '[firebase :refer [db]]
           '[devtools :refer [env kotori-info ->screen-name info-dev]])

  (def params {:db (db) :info @info-dev})

  ;;;;;;;;;;;;;
  (tweet-morning params)
  (tweet-evening params)
  (tweet-random params)

  ;;;;;;;;;;;;;
  (def info (kotori-info "0003"))
  (def result (tweet-quoted-video {:db   (db)
                                   :env  (env)
                                   :info info}))

  ;;;
  (def screen-name (->screen-name "0003"))
  (def qvt (select-next-qvt-product {:db          (db)
                                     :screen-name screen-name}))
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
