(ns kotori.procedure.kotori.core
  (:require
   [clojure.spec.alpha :as s]
   [firestore-clj.core :as f]
   [kotori.domain.kotori :as d]
   [kotori.domain.source.meigen :as meigen]
   [kotori.domain.tweet.core :as tweet]
   [kotori.domain.tweet.post :as post]
   [kotori.lib.firestore :as fs]
   [kotori.lib.json :as json]
   [kotori.lib.kotori :as lib]
   [kotori.lib.log :as log]
   [kotori.procedure.dmm.amateur :as amateur]
   [kotori.procedure.dmm.anime :as anime]
   [kotori.procedure.dmm.product :as product]
   [kotori.procedure.dmm.vr :as vr]
   [kotori.procedure.strategy.core :as st]
   [kotori.procedure.strategy.dmm :as st-dmm]
   [slingshot.slingshot :refer [throw+ try+]]
   [twitter-clj.private :as private]))

(defn make-info [{:keys [screen-name user-id auth-token ct0 proxy-map]}]
  (d/make-info screen-name user-id
               {:auth-token auth-token :ct0 ct0} proxy-map))

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

(defn tweet [{:keys [^d/Info info db text type]}]
  (let [{:keys [user-id cred proxy]} info
        text-length                  (count text)]
    (if-let [resp (handle-tweet-response
                   private/create-tweet cred proxy text)]
      (let [tweet-id (:id_str resp)
            doc-path (tweet/->post-doc-path user-id tweet-id)]
        (log/info (str "post tweet completed. id=" tweet-id
                       ", length=" text-length))
        (->> resp
             (post/->doc type)
             (fs/set! db doc-path))
        resp)
      (do
        (log/error (str "post tweet failed," " length=" text-length))
        nil))))

(defn tweet-morning
  [{:as params}]
  (tweet (assoc params :text "おはようございます" :type :text)))

(defn tweet-evening
  [{:as params}]
  (tweet (assoc params :text "今日もお疲れ様でした" :type :text)))

(defn tweet-random [{:keys [^d/Info info db env] :as params}]
  (let [source       meigen/source
        strategy     st/pick-random
        text-builder meigen/build-text
        text         (make-text source strategy text-builder)]
    (tweet (assoc params :text text :type :text))))

(defn get-product [{:as m}]
  (lib/->next (product/get-product m)))

(defn select-next-product [{:keys [db screen-name]}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (lib/->next (first (st-dmm/select-scheduled-products
                      {:db db :screen-name screen-name}))))

(defn select-next-amateur-videoa [{:keys [db screen-name]}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (lib/->next (first (st-dmm/select-scheduled-amateurs
                      {:db db :screen-name screen-name}))))

(defn select-next-amateur-videoc [{:keys [db screen-name]}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (let [next (lib/->next (first (amateur/select-scheduled-products
                                 {:db db :screen-name screen-name})))
        name (lib/videoc-title->name (:title next))]
    (assoc next :name name)))

(defn select-next-vr [{:keys [db screen-name]}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (lib/->next (first (vr/select-scheduled-products
                      {:db db :screen-name screen-name}))))

(defn select-next-anime [{:keys [db screen-name]}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (lib/->next (first (anime/select-scheduled-products
                      {:db db :screen-name screen-name}))))

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
  2. Twitterからツイートを削除.
  "
  [{:keys [^d/Info info db tweet-id]}]
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
(defn dummy [{:keys [^d/Info info db text]}]
  (assoc info :text text))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  ;;;
  (require '[firebase :refer [db db-prod db-dev]]
           '[devtools :refer [env kotori-info ->screen-name ->user-id
                              info-dev]])

  (def params {:db (db-dev) :info @info-dev})


  (tweet-morning params)
  (tweet-evening params)
  (tweet-random params)

 ;;;
  )

(comment
  (def tweet-id "xxx")
  (def params {:db (db-prod) :info (kotori-info "0001")})
  (delete-tweet! (-> params
                     (assoc :tweet-id tweet-id)))
  )

(comment
  ;;;
  (require '[devtools :refer [twitter-auth]])
  (def auth (twitter-auth))

  (def tweet (private/get-tweet (twitter-auth) "xxxxxxxx"))
  (def user (private/get-user (twitter-auth) "46130870"))
  (def resp (private/create-tweet (twitter-auth) "test"))

  (def status-id (:id_str resp))
  (def resp (private/delete-tweet (twitter-auth) status-id))
  ;;;
  )

(comment
  (def screen-name (->screen-name "0009"))
  (select-next-amateur-videoc {:db (db-prod) :screen-name screen-name})

  )

(comment
  (def resp (get-product {:db (db-prod) :cid "fcdc00141"}))
  )
