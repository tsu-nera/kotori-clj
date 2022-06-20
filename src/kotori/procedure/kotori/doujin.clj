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
   [kotori.procedure.dmm.doujin :as doujin]
   [kotori.procedure.kotori.core :as kotori]
   [twitter-clj.private :as private]))

(defn- sample->format
  [i n]
  (format "(sample %d/%d)" i n))

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

(defn- upload-images [urls creds proxy]
  (log/debug (str "upload images...cnt=" (count urls)))
  (->> urls
       io/downloads!
       (map (fn [file-path]
              {:creds     creds
               :proxy     proxy
               :file-path file-path}))
       (map private/upload-image)
       (map :media-id)
       (into [])))

(defn- make-exinfo [cid type media-ids thread-ids]
  {"cid"        cid
   "type"       type
   "media_ids"  media-ids
   "thread_ids" thread-ids})

(defn- make-page-params [base-params media-ids]
  (let  [media-ids-sep (partition-all 4 media-ids)
         page-total    (count media-ids-sep)]
    (->> media-ids-sep
         (map-indexed (fn [page-index ids]
                        {:text
                         (sample->format (+ page-index 1)
                                         page-total)
                         :media-ids (into [] ids)
                         :type      "comic"}))
         (map #(merge base-params %)))))

(defn- tweet-multi [params]
  (->> params
       (map (fn [param]
              (Thread/sleep 10000)
              (when-let [resp (kotori/tweet param)]
                (tweet/->id resp))))
       (into [])))

(defn- tweet-image [{:keys [info db coll-path] :as m}]
  (let [doc          (doujin/select-next-image m)
        urls         (into [] (rest (:urls doc)))
        media-ids    (upload-images urls (:cred info) (:proxy info))
        cid          (:cid doc)
        title        (:title doc)
        page-params  (make-page-params m media-ids)
        first-params (append-title (first page-params) title)
        rest-params  (into [] (rest page-params))]
    (when-let [thread-ids (tweet-multi (reverse rest-params))]
      (when-let [resp (kotori/tweet first-params)]
        (let [tweet-id   (tweet/->id resp)
              user-id    (:user-id info)
              doc-path   (str coll-path "/" cid)
              thread-ids (conj thread-ids tweet-id)
              exinfo     (make-exinfo cid "comic" media-ids thread-ids)]
          (doto db
            (kotori/assoc-cid! user-id tweet-id cid)
            (kotori/assoc-thread-ids! user-id tweet-id thread-ids)
            (fs/update! doc-path (product/tweet->doc resp exinfo)))
          (->discord! resp cid)
          (log/info
           (str "post tweet-image completed, cid=" cid ", title=" title)))
        resp))))

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


  (def resp2 (tweet-boys-image {:db    (db)
                                :creds (creds)
                                :info  (code->kotori "0003")}))

  (def resp3 (tweet-girls-image {:db    (db-dev)
                                 :creds (creds)
                                 :info  (code->kotori "0003")}))

  )

(comment

  (def m {:db    (db-prod)
          :creds (creds)
          :info  (code->kotori "0002")})

  (def resp (tweet-voice {:db    (db-dev)
                          :creds (creds)
                          :info  (code->kotori "0003")}))
  )
