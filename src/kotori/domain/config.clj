(ns kotori.domain.config
  (:require
   [kotori.domain.config.discord :as discord]))

(defn download-all! [db]
  (doto db
    (discord/download!)))

(comment
  (require '[firebase :refer [db-prod]])
  (download-all! (db-prod))
  )
