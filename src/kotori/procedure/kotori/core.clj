(ns kotori.procedure.kotori.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [firestore-clj.core :as f]
   [kotori.domain.dmm.product :as d-product]
   [kotori.domain.kotori.core :as d]
   [kotori.domain.source.meigen :as meigen]
   [kotori.domain.tweet.core :as tweet]
   [kotori.domain.tweet.post :as post]
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]
   [kotori.lib.json :as json]
   [kotori.lib.kotori :as lib]
   [kotori.lib.log :as log]
   [kotori.procedure.dmm.amateur :as amateur]
   [kotori.procedure.dmm.anime :as anime]
   [kotori.procedure.dmm.doujin :as doujin]
   [kotori.procedure.dmm.product :as product]
   [kotori.procedure.dmm.vr :as vr]
   [kotori.procedure.strategy.core :as st]
   [kotori.procedure.strategy.dmm :as st-dmm]
   [slingshot.slingshot :refer [throw+ try+]]
   [twitter-clj.private :as private]))

(defn make-info [{:keys [screen-name user-id code
                         auth-token ct0 proxy-map]}]
  (d/make-info screen-name user-id code
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

(defn tweet [{:keys [^d/Info info db text type media-ids reply-tweet-id]}]
  (let [{:keys [user-id cred proxy]} info
        text-length                  (count text)
        params                       {:text           text
                                      :proxies        proxy
                                      :media-ids      media-ids
                                      :reply-tweet-id reply-tweet-id}]
    (if-let [resp (handle-tweet-response
                   private/create-tweet cred params)]
      (let [tweet-id (:id_str resp)
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
  (tweet (assoc params :text "おはようございます" :type :text)))

(defn tweet-evening
  [{:as params}]
  (tweet (assoc params :text "今日もお疲れ様でした" :type :text)))

(defn- sample->format
  [i n]
  (format "(sample %d/%d)" i n))

(defn tweet-random [{:keys [^d/Info info db env] :as params}]
  (let [source       meigen/source
        strategy     st/pick-random
        text-builder meigen/build-text
        text         (make-text source strategy text-builder)]
    (tweet (assoc params :text text :type :text))))

(defn tweet-doujin-image [{:keys [^d/Info info db] :as m}]
  (let [doc           (doujin/select-next-image m)
        cid           (:cid doc)
        ;; TODO build-messageのmultimethodでreplace
        ;; 1枚目がサムネイルのことも多いがそうでなく8枚のものも多いので
        ;; 先頭から8枚をとる.
        urls          (into [] (take 8 (:urls doc)))
        media-ids     (->> urls
                           io/downloads!
                           (map (fn [file-path]
                                  {:creds     (:cred info)
                                   :proxy     (:proxy info)
                                   :file-path file-path}))
                           (map private/upload-image)
                           (map :media-id)
                           (into []))
        exinfo        {"cid" cid "media_ids" media-ids}
        ;; TODO リファクタリングが必要.
        media-ids-sep (partition 4 media-ids)
        media-ids-1   (first media-ids-sep)
        media-ids-2   (second media-ids-sep)
        total         (if (< (count media-ids-2) 4) 1 2)
        message-1     (str (:title doc) " " cid
                           "\n" (sample->format 1 total))
        params-1      (merge m {:text      message-1
                                :type      :comic ;; TODO 仮対応
                                :media-ids media-ids-1})
        message-2     (sample->format 2 total)
        params-2      (merge m {:text      message-2
                                :type      :comic ;; TODO 仮対応
                                :media-ids media-ids-2})]
    (when-let [resp (tweet params-1)]
      ;; リプライ投稿は画像があるときだけ.
      (when (= 2 total)
        (let [tweet-id (:id_str resp)]
          (tweet (assoc params-2 :reply-tweet-id tweet-id))))
      (let [doc-path (d-product/doujin-doc-path cid)]
        (fs/update! db doc-path (d-product/tweet->doc resp exinfo)))
      resp)))

(defn get-product [{:as m}]
  (lib/->next (product/get-product m)))

(defn select-next-product [{:keys [screen-name] :as m}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (lib/->next (first (st-dmm/select-scheduled-products m))))

(defn select-next-amateur-videoc [{:keys [screen-name] :as m}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (let [next (lib/->next (first (amateur/select-scheduled-products m)))
        name (lib/videoc-title->name (:title next))]
    (assoc next :name name)))

(defn select-next-vr [{:keys [screen-name] :as m}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (lib/->next (first (vr/select-scheduled-products m))))

(defn select-next-anime [{:keys [screen-name] :as m}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (lib/->next (first (anime/select-scheduled-products m))))

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
           '[tools.dmm :refer [creds]]
           '[devtools :refer [kotori-info ->screen-name
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
  (def params {:db (db-prod) :info (kotori-info "0001")})
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
  (def info (kotori-info "0001"))
  (def resp (select-next-product {:db          (db-prod)
                                  :screen-name (:screen-name info)
                                  :creds       (creds)
                                  :limit       100
                                  :info        info}))
  )

(comment
  (def info (kotori-info "0027"))
  (def resp (select-next-amateur-videoc
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

(comment
  (def resp (doujin/select-next-image {:db    (db)
                                       :creds (creds)
                                       :info  (kotori-info "0003")}))

  (def urls (into [] (take 8 (rest (:urls resp)))))
  (def paths (->> (range 1 5)
                  (map (fn [n] (str "tmp/image-" n ".jpg")))))


  (mapcat io/download! urls paths)

  (doseq [url  urls
          path paths]
    (println url path)
    #_(io/download! url path))

  (def image-paths (io/downloads! urls))

  (def resp2 (tweet-doujin-image {:db    (db)
                                  :creds (creds)
                                  :info  (kotori-info "0003")}))

  (def ret (tweet resp2))

  (def media-ids (->> urls
                      io/downloads!
                      (map (fn [file-path]
                             {:creds     (twitter-auth)
                              :file-path file-path}))
                      (map private/upload-image)
                      (map :media-id)))
  )
