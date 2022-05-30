(ns kotori.procedure.kotori.doujin
  (:require
   [kotori.domain.dmm.product :as product]
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]
   [kotori.procedure.dmm.doujin :as doujin]
   [kotori.procedure.kotori.core :as kotori]
   [twitter-clj.private :as private]))

(defn- sample->format
  [i n]
  (format "(sample %d/%d)" i n))

(defn tweet-image [{:keys [info db] :as m}]
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
    (when-let [resp (kotori/tweet params-1)]
      ;; リプライ投稿は画像があるときだけ.
      (when (= 2 total)
        (let [tweet-id (:id_str resp)]
          (kotori/tweet (assoc params-2 :reply-tweet-id tweet-id))))
      (let [doc-path (product/doujin-doc-path cid)]
        (fs/update! db doc-path (product/tweet->doc resp exinfo)))
      resp)))

(defn- ->otameshi [urls i]
  (str "お試し"
       (+ 1 i)
       "🔞\n"
       (nth urls i)
       "\n"))

(defn make-doujin-voice-text [doc urls]
  (let [new-line   "\n\n"
        sample-max (count urls)
        title      (:title doc)
        af-url     (:affiliate-url doc)]
    (str "[voice]"
         title
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
        exinfo  {"cid" cid}
        ;; TODO リファクタリングが必要.
        message (make-doujin-voice-text doc urls)
        params  (merge m {:text message
                          :type :voice ;; TODO 仮対応
                          })]
    (when-let [resp (kotori/tweet params)]
      (let [doc-path (product/doujin-doc-path cid)]
        (fs/update! db doc-path (product/tweet->doc resp exinfo)))
      resp)))

(comment
  (require '[firebase :refer [db db-prod db-dev]]
           '[tools.dmm :refer [creds]]
           '[devtools :refer [kotori-info ->screen-name
                              info-dev twitter-auth]])

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

  (def resp2 (tweet-image {:db    (db)
                           :creds (creds)
                           :info  (kotori-info "0003")}))
  )

(comment

  (def m {:db    (db-prod)
          :creds (creds)
          :info  (kotori-info "0003")})

  (def doc (doujin/select-next-voice m))
  (def resp (tweet-voice {:db    (db)
                          :creds (creds)
                          :info  (kotori-info "0003")}))


  )
