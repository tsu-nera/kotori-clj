(ns kotori.procedure.kotori.core
  (:require
   [clojure.spec.alpha :as s]
   [firestore-clj.core :as f]
   [kotori.domain.kotori.core :as d]
   [kotori.domain.source.meigen :as meigen]
   [kotori.domain.tweet.core :as tweet]
   [kotori.domain.tweet.post :as post]
   [kotori.lib.firestore :as fs]
   [kotori.lib.json :as json]
   [kotori.lib.kotori :as lib]
   [kotori.lib.log :as log]
   [kotori.lib.provider.dmm.public :as public]
   [kotori.procedure.dmm.amateur :as amateur]
   [kotori.procedure.dmm.anime :as anime]
   [kotori.procedure.dmm.product :as product]
   [kotori.procedure.dmm.vr :as vr]
   [kotori.procedure.strategy.core :as st]
   [kotori.procedure.strategy.dmm :as st-dmm]
   [slingshot.slingshot :refer [throw+ try+]]
   [twitter-clj.private :as private]))

(defn config->kotori [{:keys [screen-name user-id code proxy-info]
                       :as   m}]
  (let [cred-map (d/config->cred-map m)]
    (d/make-info screen-name user-id code
                 cred-map
                 proxy-info)))

(defn make-text [source strategy builder]
  (builder (strategy source)))

(defn handle-tweet-response
  [req-fn & req]
  (try+
   (apply req-fn req)
   (catch [:status 403] {:keys [body]}
     (let [error   (first (:errors (-> body json/parse-string json/->clj)))
           code    (:code error)
           message (:message error)]
       (log/warn (str "403 Forbidden, " message " code=" code)))
     nil)
   (catch Object _
     (log/error (:throwable &throw-context) "unexpected error")
     (throw+))))

(defn- assoc-thread-id!
  "ツイートの親子関係をtweet-idでさかのぼれるように双方向リンクしとく"
  [db user-id parent-id child-id]
  (let [parent-doc-path
        (tweet/->post-doc-path user-id parent-id)
        child-doc-path
        (tweet/->post-doc-path user-id child-id)]
    (doto db
      (fs/assoc! child-doc-path
                 "self_replying_tweet_id" parent-id)
      (fs/assoc! parent-doc-path
                 "self_replyed_tweet_id" child-id))))

(defn assoc-cid!
  [db user-id tweet-id cid]
  (let [doc-path (tweet/->post-doc-path user-id tweet-id)]
    (fs/assoc! db doc-path "cid" cid)))

(defn assoc-thread-ids!
  "一つのtweet-idのフィールドに関連idのリストをまとめてbind"
  [db user-id tweet-id thread-ids]
  (let [doc-path (tweet/->post-doc-path user-id tweet-id)]
    (fs/assoc! db doc-path "thread_ids" thread-ids)))

(defn tweet [{:keys [^d/Kotori info db text type media-ids reply-tweet-id]}]
  (let [{:keys [user-id cred proxy]} info
        text-length                  (count text)
        params                       {:text           text
                                      :proxies        proxy
                                      :media-ids      media-ids
                                      :reply-tweet-id reply-tweet-id}]
    (if-let [resp (handle-tweet-response
                   private/create-tweet cred params)]
      (let [tweet-id (tweet/->id resp)
            doc-path (tweet/->post-doc-path user-id tweet-id)]
        (log/info (str "post tweet completed. id=" tweet-id
                       ", length=" text-length ",media-ids=" media-ids))
        (->> resp
             (post/->doc type)
             (fs/set! db doc-path))
        (when reply-tweet-id
          (assoc-thread-id! db user-id reply-tweet-id tweet-id))
        resp)
      (do
        (log/error (str "post tweet failed," " length=" text-length))
        nil))))

(defn tweet-morning
  [{:as params}]
  (tweet (assoc params :text "おはようございます" :type "text")))

(defn tweet-evening
  [{:as params}]
  (tweet (assoc params :text "今日もお疲れ様でした" :type "text")))

(defn tweet-random [{:keys [^d/Kotori info db env] :as params}]
  (let [source       meigen/source
        strategy     st/pick-random
        text-builder meigen/build-text
        text         (make-text source strategy text-builder)]
    (tweet (assoc params :text text :type "text"))))

(defn get-product [{:as m}]
  (lib/->next (product/get-product m)))

(defn select-next-product [{:keys [info screen-name] :as m}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (let [af-id (d/kotori->af-id info)]
    (-> m
        st-dmm/select-scheduled-products
        first
        lib/->next
        (lib/next->swap-af-id af-id))))

(defn select-next-amateur-videoc [{:keys [info screen-name] :as m}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (let [af-id (d/kotori->af-id info)
        next  (-> m
                  amateur/select-scheduled-products
                  first
                  lib/->next)]
    (-> next
        (assoc :name (lib/videoc-title->name (:title next)))
        (lib/next->swap-af-id af-id))))

(defn select-next-vr [{:keys [info screen-name] :as m}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (when-let [next (-> m
                      vr/select-scheduled-products
                      first
                      lib/->next
                      (lib/next->swap-af-id (d/kotori->af-id info)))]
    (let [cid (:cid next)
          uri (public/cid->vr-uri cid)]
      (cond-> next
        (public/uri-exists? uri) (assoc :sample-movie-url uri)))))

(defn select-next-anime [{:keys [info screen-name] :as m}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (let [af-id (d/kotori->af-id info)]
    (-> m
        anime/select-scheduled-products
        first
        lib/->next
        (lib/next->swap-af-id af-id))))

(defn archive-fs-tweet-data [db user-id tweet-id]
  (f/transact!
   db
   (fn [tx]
     (let [post-doc (f/doc db (tweet/->post-doc-path user-id tweet-id))
           archive-doc
           (f/doc db (tweet/->archive-doc-path user-id tweet-id))
           data     (-> (f/pull-doc post-doc tx) post/->archive-data)]
       (f/delete tx post-doc)
       (f/set tx archive-doc data)
       data))))

(defn delete-tweet! "
  1. tweets/:user_id/posts/:status_id を tweets/:user_id/archivesへ移動.
  2. Twitterからツイートを削除."
  [{:keys [^d/Kotori info db tweet-id]}]
  (let
   [{:keys [user-id cred proxy]} info]
    (try
      (archive-fs-tweet-data db user-id tweet-id)
      (when-let [resp (private/delete-tweet cred proxy tweet-id)]
        (log/info (str "delete tweet completed. id=" tweet-id))
        resp)
      (catch Exception e
        (log/error "delete tweet Failed." (.getMessage e))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn dummy [{:keys [Kotorinfo info db text]}]
  (assoc info :text text))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  ;;;
  (require '[firebase :refer [db db-prod db-dev]]
           '[tools.dmm :refer [creds]]
           '[devtools :refer [code->kotori ->screen-name
                              info-dev twitter-auth]])

  (def params {:db (db-dev) :info @info-dev})

  (tweet-morning params)
  (tweet-evening params)
  (tweet-random params)

  (def reply-tweet-id "xx")
  (def resp (tweet (merge params {:text           "リプテスト3"
                                  :reply-tweet-id reply-tweet-id})))

 ;;;
  )

(comment
  (def tweet-id "xxx")
  (def params {:db (db-prod) :info (code->kotori "0001")})
  (delete-tweet! (-> params
                     (assoc :tweet-id tweet-id)))
  )

(comment
  ;;;

  (def tweet (private/get-tweet (twitter-auth) "xxxxxxxx"))
  (def user (private/get-user (twitter-auth) "46130870"))
  (def resp (private/create-tweet (twitter-auth) {:text "test"}))

  (def status-id (:id_str resp))
  (def resp (private/delete-tweet (twitter-auth) status-id))
  ;;;
  )

(comment
  (def info (code->kotori "0001"))
  (def resp (select-next-product {:db          (db-prod)
                                  :screen-name (:screen-name info)
                                  :creds       (creds)
                                  :limit       100
                                  :info        info}))
  )

(comment
  (def info (code->kotori "0027"))
  (def resp (select-next-amateur-videoc
             {:db          (db-prod)
              :creds       (creds)
              :info        info
              :screen-name (:screen-name info)}))
  )

(comment
  (def info (code->kotori "0028"))
  (def resp (select-next-vr
             {:db          (db-prod)
              :creds       (creds)
              :info        info
              :screen-name (:screen-name info)}))
  )

(comment
  (def resp (get-product {:db (db-prod) :cid "lzdm00050"}))
  (count (:description resp))
  )

(comment
  (def resp (private/create-tweet (twitter-auth) {:text "test"}))
  (def media-ids [])
  (def resp (private/create-tweet (twitter-auth)
                                  {:text      "test3"
                                   :media-ids media-ids}))
  )

