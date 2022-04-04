(ns kotori.procedure.tweet.post
  (:require
   [firestore-clj.core :as f]
   [kotori.domain.tweet.post :as m]
   [kotori.lib.firestore :as fs]
   [kotori.lib.time :as t]))

(defn get-video-posts [{:keys [user-id db since-weeks days]}]
  (let [base-time (t/date->weeks-ago since-weeks)
        from-time (t/->fs-timestamp base-time)
        to-time   (t/->fs-timestamp (t/date->days-later days base-time))
        xquery
        (fs/make-xquery [(fs/query-filter "media_type", "video")
                         (fs/query-order-by "created_at")
                         (fs/query-start-at from-time)
                         (fs/query-end-before to-time)])]
    (fs/get-docs db
                 (m/->coll-path user-id)
                 xquery)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[firebase :refer [db-prod]])
  (require '[firestore-clj.core :as f])
  )
