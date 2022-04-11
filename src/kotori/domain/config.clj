(ns kotori.domain.config
  (:require
   [kotori.domain.discord :as discord]
   [kotori.domain.source.meigen :as meigen]))

(defn download-all! [db]
  (doto db
    (meigen/download!)
    (discord/download!)))

(comment
  (require '[firebase :refer [db-prod]])
  (download-all! (db-prod))
  )
