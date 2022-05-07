(ns kotori.procedure.kotori.qvt
  "動画引用ツイート処理"
  (:require
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.product :as product]
   [kotori.domain.tweet.core :as tweet]
   [kotori.domain.tweet.qvt :as qvt]
   [kotori.lib.discord :as discord]
   [kotori.lib.firestore :as fs]
   [kotori.lib.log :as log]
   [kotori.lib.provider.dmm.public :as public]
   [kotori.procedure.kotori.core :as kotori]
   [kotori.procedure.strategy.core :as st]
   [kotori.procedure.strategy.dmm :as st-dmm]))

(defn select-next-qvt-product [{:as params}]
  (when-let [product (first (st-dmm/select-tweeted-products params))]
    (qvt/doc-> product)))

(defn get-qvt [{:keys [db cid]}]
  (let [doc-path (product/doc-path cid)
        doc      (fs/get-doc db doc-path)]
    (qvt/doc-> doc)))

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

(defn- assoc-desc-unless
  "descriptionが存在しない場合はここで取得
  一つのページへのアクセスは高速なのでこのタイミングで問題ない."
  [qvt]
  (if (not (:description qvt))
    (let [cid (:cid qvt)
          ;; FIXME とりあえずqvtでのvideoa以外の対応はあとで.
          params {:cid cid :floor (:videoa dmm/floor)}
          page   (public/get-page params)
          desc   (:description page)]
      (assoc qvt :description desc))
    qvt))

(defn- qvt-url? [qvt]
  (and qvt (:url qvt)))

(defn- update-product-with-qvt! [db result qvt cid]
  (let [doc-path (product/doc-path cid)]
    (fs/update! db doc-path (product/qvt->doc qvt result))))

(defn- update-post-with-qvt! [db result qvt user-id]
  (let [tweet-id (:id_str result)
        doc-path (tweet/->post-doc-path user-id tweet-id)]
    (->> qvt qvt/->doc (fs/update! db doc-path))))

(defn tweet-quoted-video
  ([{:keys [^d/Info info db] :as params}]
   (let [screen-name (:screen-name info)
         qvt         (select-next-qvt-product
                      {:db db :screen-name screen-name})]
     (tweet-quoted-video params (assoc-desc-unless qvt))))
  ([{:keys [^d/Info info db source-label message-type]
     :as   params} qvt]
   (let [cid          (:cid qvt)
         user-id      (:user-id info)
         source       (qvt/get-source source-label)
         strategy     st/pick-random
         text-builder (fn [data] (qvt/build-text qvt message-type data))
         text         (kotori/make-text source strategy text-builder)
         tweet-params (assoc params :text text :type :qvt)]
     (if (qvt-url? qvt)
       (if-let [result (kotori/tweet tweet-params)]
         (do
           ;; DMM商品情報 collectionを更新.
           (update-product-with-qvt! db result qvt cid)
           ;; tweets collectionも追加情報でupdate
           (update-post-with-qvt! db result qvt user-id)
           ;; crawledされてない場合はここで追加で処理をする.
           ;; 通常はcrawledされているのでtoolで追加した場合がこうなる.
           ;; そんなに時間かからないと思うので同期処理
           #_(when-not (:craweled? qvt)
               (dmm/crawl-product! {:db db :env env :cid cid}))
           ;; discord通知
           (qvt->discord! qvt result)
           result)
         {:result :failed})
       (do
         (log/error "quoted video url not found.")
         {:result :failed})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[firebase :refer [db db-dev db-prod]]
           '[devtools :refer [env ->screen-name kotori-info]])

   ;;;;;;;;;;;;;
  (def info (kotori-info "0003"))
  (def screen-name (->screen-name "0003"))

  (def qvt (select-next-qvt-product {:db          (db-dev)
                                     :screen-name screen-name}))

  (def result (tweet-quoted-video {:db           (db-dev)
                                   :env          (env)
                                   :info         info
                                   :source-label "qvt_0003"
                                   :message-type "description"}))

 ;;;
  (def qvt-data (product/qvt->doc qvt result))
 ;;;
  (def cid "mide00897")
  (def qvt (get-qvt {:db (db-dev) :cid cid}))

  (def desc (:description qvt))
  (count desc)

  (def result (tweet-quoted-video {:db           (db-dev)
                                   :env          (env)
                                   :info         info
                                   :source-label "qvt_0003"
                                   :message-type "description"} qvt))
  )
