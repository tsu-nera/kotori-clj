(ns kotori.procedure.kotori.doujin
  (:require
   [kotori.domain.dmm.genre.doujin :as genre]
   [kotori.domain.dmm.product :as product]
   [kotori.domain.tweet.core :as tweet]
   [kotori.lib.discord :as discord]
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]
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

(defn tweet-image [{:keys [info db] :as m}]
  (let [doc           (doujin/select-next-image m)
        cid           (:cid doc)
        ;; TODO build-messageのmultimethodでreplace
        ;; 1枚目がサムネイルのことも多いがそうでなく8枚のものも多いので
        ;; 先頭から8枚をとる.
        urls          (into [] (take 4 (:urls doc)))
        media-ids     (->> urls
                           io/downloads!
                           (map (fn [file-path]
                                  {:creds     (:cred info)
                                   :proxy     (:proxy info)
                                   :file-path file-path}))
                           (map private/upload-image)
                           (map :media-id)
                           (into []))
        exinfo        {"cid"       cid
                       "media_ids" media-ids
                       "type"      "comic" ;; TODO 仮対応
                       }
        ;; TODO リファクタリングが必要.
        media-ids-sep (partition 2 media-ids)
        media-ids-1   (first media-ids-sep)
        media-ids-2   (second media-ids-sep)
        total         (if (< (count media-ids-2) 2) 1 2)
        message-1     (str (:title doc) " " cid
                           "\n" (sample->format 1 total))
        params-1      (merge m {:text      message-1
                                :media-ids media-ids-1
                                :type      "comic"})
        message-2     (sample->format 2 total)
        params-2      (merge m {:text      message-2
                                :media-ids media-ids-2})]
    (when-let [resp (kotori/tweet params-1)]
      ;; リプライ投稿は画像があるときだけ.
      (when-not (nil? media-ids-2)
        (let [tweet-id (:id_str resp)]
          (kotori/tweet (assoc params-2 :reply-tweet-id tweet-id))))
      (let [doc-path (genre/->doc-path cid)]
        (fs/update! db doc-path (product/tweet->doc resp exinfo))
        (->discord! resp cid))
      resp)))

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
        (->discord! resp cid))
      resp)))

(comment
  (require '[firebase :refer [db db-prod db-dev]]
           '[tools.dmm :refer [creds]]
           '[devtools :refer [kotori-info ->screen-name
                              info-dev twitter-auth]])

  (def resp (doujin/select-next-image {:db    (db-prod)
                                       :creds (creds)
                                       :info  (kotori-info "0029")}))

  (def urls (into [] (take 8 (rest (:urls resp)))))
  (def paths (->> (range 1 5)
                  (map (fn [n] (str "tmp/image-" n ".jpg")))))

  (mapcat io/download! urls paths)
  (doseq [url  urls
          path paths]
    (println url path)
    #_(io/download! url path))

  (def image-paths (io/downloads! urls))

  (def resp2 (tweet-image {:db    (db-prod)
                           :creds (creds)
                           :info  (kotori-info "0029")}))
  )

(comment

  (def m {:db    (db-prod)
          :creds (creds)
          :info  (kotori-info "0002")})

  (def resp (tweet-voice {:db    (db-prod)
                          :creds (creds)
                          :info  (kotori-info "0002")}))
  )

