(ns kotori.procedure.tweet.post
  (:require
   [firestore-clj.core :as f]
   [kotori.domain.tweet.core :as tweet]
   [kotori.lib.firestore :as fs]
   [kotori.lib.time :as t]))

(defn get-video-posts [{:keys [user-id db days-ago days]}]
  (let [base-time (t/date->days-ago days-ago)
        from-time (t/->fs-timestamp base-time)
        to-time   (t/->fs-timestamp (t/date->days-later days base-time))
        xquery
        (fs/make-xquery [(fs/query-filter "media_type", "video")
                         (fs/query-order-by "created_at")
                         (fs/query-start-at from-time)
                         (fs/query-end-before to-time)])]
    (fs/get-docs db
                 (tweet/->post-coll-path user-id)
                 xquery)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[firebase :refer [db-prod]])
  (require '[firestore-clj.core :as f])
  )
