(ns kotori.domain.config.ngword
  (:require
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]))

(def doc-path "configs/ngword")
(def file-path "private/ngword.edn")

(def source (io/load-edn file-path))

(defn download! [db]
  (->> doc-path
       (fs/get-doc-raw db)
       (io/dump-edn! file-path)))

(defn upload! [db]
  (fs/set-raw! db doc-path source))

(comment
  (require '[firebase :refer [db-dev db-prod]])

  (download! (db-dev))
  (upload! (db-prod))
  )
