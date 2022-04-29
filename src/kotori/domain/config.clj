(ns kotori.domain.config
  (:require
   [kotori.domain.config.discord :as discord]
   [kotori.domain.config.ngword :as ngword]))

(defn download-all! [db]
  (doto db
    (discord/download!)
    (ngword/download!)))

(defn upload-all! [db]
  (doto db
    (discord/upload!)
    (ngword/upload!)))

(comment
  (require '[firebase :refer [db-prod]])
  (download-all! (db-prod))
  )
