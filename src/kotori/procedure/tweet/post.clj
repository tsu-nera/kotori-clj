(ns kotori.procedure.tweet.post
  (:require
   [kotori.domain.tweet.post :as m]
   [kotori.lib.firestore :as fs]
   [kotori.lib.time :as time]))

(defn get-video-posts [{:keys [user-id db weeks]}]
  (let [past-time (time/->fs-timestamp (time/weeks-ago weeks))
        xquery
        (fs/make-xquery [(fs/query-filter "media_type", "video")
                         (fs/query-more "created_at" past-time)])]
    (fs/get-docs db
                 (m/->coll-path user-id)
                 xquery)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

