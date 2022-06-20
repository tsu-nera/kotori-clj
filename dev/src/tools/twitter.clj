(ns tools.twitter
  (:require
   [clojure.pprint :refer [pprint]]
   [devtools :refer [->screen-name ->user-id kotori-params twitter-auth]]
   [firebase :refer [db-dev db-prod]]
   [kotori.domain.tweet.core :as tweet]
   [kotori.lib.firestore :as fs]
   [kotori.lib.time :as time]
   [kotori.procedure.kotori.core :as kotori]
   [twitter-clj.guest :as guest]
   [twitter-clj.private :as private]))

(defn get-tweet-guest [id]
  (guest/get-tweet id))

(defn get-tweet-private [id]
  (private/get-tweet (twitter-auth) id))

(defn tweet-id->media-ids [id]
  (when-let [resp (get-tweet-private id)]
    (->> resp
         :entities
         :media
         (map :id_str))))

(defn tweet-id->studio-video-url [id]
  (let [media-id (first (tweet-id->media-ids id))]
    (str "https://studio.twitter.com/library/7_" media-id "/edit")))

(defn delete-tweet!
  ([code tweet-id]
   (delete-tweet! (db-prod) code tweet-id))
  ([db code tweet-id]
   (let [params   (kotori-params db code)
         user-id  (get-in params [:info :user-id])
         doc-path (tweet/->post-doc-path user-id tweet-id)]
     (when-let [doc (fs/get-doc db doc-path)]
       (if-let [thread-ids (:thread-ids doc)]
         (doseq [thread-id thread-ids]
           (kotori/delete-tweet!
            (assoc params :tweet-id thread-id))
           (time/sleep! 2))
         (kotori/delete-tweet!
          (assoc params :tweet-id tweet-id)))
       (if-let [cid (:cid doc)]
         ;; TODO kotoriの中に coll-pathをいれる対応が必要.
         ;; 削除したものに対する排他も検討が必要.
         (println cid)
         (println "cid not found"))))))
#_(delete-tweet! (db-dev) "0003" "")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[twitter-clj.media :as media])

  (def sample-path "../../Downloads/d_227233jp-001.jpg")

  (private/->total-bytes sample-path)
  (private/create-tweet (twitter-auth) {:text "test3"})

  (def resp-init
    (media/init {:creds     (twitter-auth)
                 :file-path sample-path}))

  (def media-id (:media-id resp-init))

  (def resp-append
    (media/append {:creds     (twitter-auth)
                   :media-id  (:media-id resp-init)
                   :file-path sample-path}))

  (def resp-finalize
    (media/finalize {:creds    (twitter-auth)
                     :media-id media-id}))

  ;; 画像の場合はエラー.
  (def resp-status
    (media/status {:creds    (twitter-auth)
                   :media-id media-id}))

  (def ret (private/upload-image {:creds     (twitter-auth)
                                  :file-path sample-path}))

  (private/create-tweet (twitter-auth) {:text      "test4"
                                        :media-ids [(:media-id ret)]})
  )

(comment
  (def resp (tweet-id->studio-video-url ""))
  )

(comment

  (def screen-name "gmdtz")
  (def resp (private/get-user screen-name))

  )
