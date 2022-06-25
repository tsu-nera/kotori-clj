(ns kotori.procedure.kotori.doujin
  (:require
   [kotori.domain.dmm.genre.doujin :as genre]
   [kotori.domain.dmm.product :as product]
   [kotori.domain.tweet.core :as tweet]
   [kotori.lib.discord :as discord]
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]
   [kotori.lib.log :as log]
   [kotori.lib.provider.dmm.doujin :refer [->url]]
   [kotori.lib.provider.dmm.parser :as perser]
   [kotori.lib.time :as time]
   [kotori.procedure.dmm.doujin :as doujin]
   [kotori.procedure.kotori.core :as kotori]
   [twitter-clj.private :as private]))

(defn- sample->format
  [i n]
  (format "(%d/%d)" i n))

(defn ->discord! [tweet cid]
  (let [screen-name (tweet/->screen-name tweet)
        tweet-id    (tweet/->id tweet)
        tweet-link  (tweet/->url screen-name tweet-id)
        dmm-url     (->url cid)
        message     (str screen-name " post tweet completed.\n"
                         dmm-url "\n"
                         tweet-link "\n")]
    (discord/notify! :kotori-post message)))

(defn- append-title [params title]
  (update params :text (fn [s]
                         (str title "\n" s))))

(defn- upload-images [urls creds proxy-info]
  (log/debug (str "upload images...cnt=" (count urls)))
  (->> urls
       io/downloads!
       (map (fn [file-path]
              {:creds     creds
               :proxy     proxy-info
               :file-path file-path}))
       (map private/upload-image)
       (map :media-id)
       (into [])))

(defn- make-exinfo [cid type media-ids thread-ids]
  {"cid"        cid
   "type"       type
   "media_ids"  media-ids
   "thread_ids" thread-ids})

(defn- calc-page-total [media-ids sep]
  (count (partition-all sep media-ids)))

(defn- make-root-params [base-params title thumbnail-id page-total url]
  (let [text (str title "\n(全" page-total "ページ)" "\n" url)]
    (merge base-params {:text      text
                        :media-ids [thumbnail-id]
                        :type      "comic"})))

(defn- make-page-params [base-params media-ids sep page-total]
  (let  [media-ids-sep (partition-all sep media-ids)]
    (->> media-ids-sep
         (map-indexed (fn [page-index ids]
                        {:text
                         (sample->format (+ page-index 1)
                                         page-total)
                         :media-ids (into [] ids)
                         :type      "comic"}))
         (map #(merge base-params %)))))

(defn- tweet-thread [root-id thread-params]
  (loop [parent-id root-id
         params    thread-params
         acc       []]
    (let [m-first (first params)
          m-rest  (rest params)]
      (if (nil? m-first)
        acc
        (let [m (-> m-first
                    (assoc :reply-tweet-id parent-id))]
          (time/sleep! 5)
          (let [resp     (kotori/tweet m)
                tweet-id (tweet/->id resp)]
            (recur tweet-id
                   m-rest
                   (conj acc tweet-id))))))))

(defn- tweet-image [{:keys [info db coll-path] :as m}]
  (let [doc            (doujin/select-next-image m)
        urls           (into [] (:urls doc))
        media-ids      (upload-images urls (:cred info) (:proxy-info info))
        cid            (:cid doc)
        title          (:title doc)
        af-url         (:affiliate-url doc)
        sep            4
        thumbnail-id   (first media-ids)
        page-media-ids (into [] (rest media-ids))
        page-total     (calc-page-total page-media-ids sep)
        root-params    (make-root-params m title
                                         thumbnail-id page-total af-url)
        page-params    (make-page-params m page-media-ids sep page-total)]
    (when-let [resp (kotori/tweet root-params)]
      (let [root-id (tweet/->id resp)]
        (when-let [thread-ids (tweet-thread root-id page-params)]
          (let [user-id    (:user-id info)
                doc-path   (str coll-path "/" cid)
                thread-ids (conj thread-ids root-id)
                exinfo     (make-exinfo cid "comic" media-ids thread-ids)]
            (doto db
              (kotori/assoc-cid! user-id root-id cid)
              (kotori/assoc-thread-ids! user-id root-id thread-ids)
              (fs/update! doc-path (product/tweet->doc resp exinfo)))
            (->discord! resp cid)
            (log/info
             (str "post tweet-image completed, cid=" cid ", title=" title)))
          resp)))))

;; TODO インタフェースで解決する.
(defn tweet-boys-image [{:as m}]
  (tweet-image
   (merge m {:genre-id  genre/for-boy-id
             :coll-path product/doujin-coll-path})))

;; TODO インタフェースで解決する.
(defn tweet-girls-image [{:as m}]
  (tweet-image
   (merge m {:genre-id  genre/for-girl-id
             :coll-path product/girls-coll-path})))

(defn- ->otameshi [urls i]
  (str "お試し"
       (+ 1 i)
       "🔞\n"
       (nth urls i)
       "\n"))

(defn make-voice-text [doc urls]
  (let [new-line   "\n\n"
        sample-max (count urls)
        title      (perser/trunc 60 (:title doc))
        af-url     (:affiliate-url doc)]
    (str title
         new-line
         (->otameshi urls 0)
         (when (< 1 sample-max)
           (->otameshi urls 1))
         "\n"
         (when (< 2 sample-max)
           (str "無料サンプル音声は全部で" sample-max "つ✨"
                new-line))
         "⬇️続きはコチラ\n" af-url)))

(defn tweet-voice [{:keys [info db] :as m}]
  (let [doc     (doujin/select-next-voice m)
        title   (:title doc)
        cid     (:cid doc)
        urls    (into [] (:urls doc))
        exinfo  {"cid"  cid
                 "type" "voice" ;; TODO 仮対応
                 }
        ;; TODO リファクタリングが必要.
        message (make-voice-text doc urls)
        params  (-> m
                    (assoc :text message)
                    (assoc :type "voice"))]
    (when-let [resp (kotori/tweet params)]
      (let [doc-path (genre/->doc-path cid)]
        (fs/update! db doc-path (product/tweet->doc resp exinfo))
        (log/info
         (str "post tweet-voice completed, cid=" cid ", title=" title))
        (->discord! resp cid))
      resp)))

(comment
  (require '[firebase :refer [db db-prod db-dev]]
           '[tools.dmm :refer [creds]]
           '[devtools :refer [code->kotori ->screen-name
                              info-dev twitter-auth]])

  (def resp (doujin/select-next-image {:db    (db-prod)
                                       :creds (creds)
                                       :info  (code->kotori "0029")}))
  (def urls (into [] (rest (:urls resp))))

  (def media-ids-sep (partition-all 2 urls))
  (def page-total (count media-ids-sep))

  (def page-params (map-indexed (fn [page-index ids]
                                  {:text      (sample->format (+ page-index 1)
                                                              page-total)
                                   :media-ids ids
                                   :type      "comic"})
                                media-ids-sep))

  (def title (:title resp))
  (def params (append-title (first page-params) title))

  (def rest-params (into [] (rest page-params)))


  (def resp2 (tweet-boys-image {:db    (db-prod)
                                :creds (creds)
                                :info  (code->kotori "0029")}))

  (def resp3 (tweet-girls-image {:db    (db-prod)
                                 :creds (creds)
                                 :info  (code->kotori "0026")}))

  )

(comment

  (def m {:db    (db-prod)
          :creds (creds)
          :info  (code->kotori "0002")})

  (def resp (tweet-voice {:db    (db-dev)
                          :creds (creds)
                          :info  (code->kotori "0003")}))
  )
