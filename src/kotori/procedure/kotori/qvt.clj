(ns kotori.procedure.kotori.qvt
  "動画引用ツイート処理"
  (:require
   [kotori.domain.dmm.product :as product]
   [kotori.domain.tweet.core :as tweet]
   [kotori.domain.tweet.qvt :as qvt]
   [kotori.lib.discord :as discord]
   [kotori.lib.firestore :as fs]
   [kotori.procedure.dmm :as dmm]
   [kotori.procedure.kotori.core :as kotori]
   [kotori.procedure.strategy.core :as st]
   [kotori.procedure.strategy.dmm :as st-dmm]))

;; いったん挿入をやめるのでマスク
#_(defn- scrape-desc-if-not-exists [product]
    (if-not (:description product)
      (let [cid  (:cid product)
            page (dmm/scrape-page {:cid cid})
            desc (:description page)]
        (assoc product :description desc))
      product))

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

(defn tweet-quoted-video
  ([{:keys [^d/Info info db] :as params}]
   (let [screen-name (:screen-name info)
         qvt         (select-next-qvt-product
                      {:db db :screen-name screen-name})]
     (tweet-quoted-video params qvt)))
  ([{:keys [db env source-label message-type] :as params} qvt]
   (let [cid          (:cid qvt)
         source       (qvt/get-source source-label)
         strategy     st/pick-random
         text-builder (fn [data] (qvt/build-text qvt message-type data))
         text         (kotori/make-text source strategy text-builder)
         doc-path     (product/doc-path cid)
         crawled?     (:craweled? qvt)
         tweet-params (assoc params :text text :type :qvt)]
     (if (and qvt (:url qvt))
       (when-let [result (kotori/tweet tweet-params)]
         ;; DMM商品情報 collectionを更新.
         (->> result
              (qvt/->doc qvt)
              ;; dmm/products/{cid} の情報を更新
              (fs/update! db doc-path))
         ;; crawledされてない場合はここで追加で処理をする.
         ;; 通常はcrawledされているのでtoolで追加した場合がこうなる.
         ;; そんなに時間かからないと思うので同期処理
         (when-not crawled?
           (dmm/crawl-product! {:db db :env env :cid cid}))
         (qvt->discord! qvt result)
         result)
       (do
         (println "quoted video url not found.")
         {})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[firebase :refer [db db-dev db-prod]]
           '[devtools :refer [env ->screen-name kotori-info]])

   ;;;;;;;;;;;;;
  (def info (kotori-info "0003"))
  (def result (tweet-quoted-video {:db           (db)
                                   :env          (env)
                                   :info         info
                                   :source-label "qvt_0003"}))

 ;;;
  (def screen-name (->screen-name "0023"))
  (def qvt (select-next-qvt-product {:db          (db-prod)
                                     :screen-name screen-name}))


  (def qvt-data (qvt/->doc qvt result))
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
