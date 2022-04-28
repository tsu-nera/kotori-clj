(ns kotori.domain.config.discord
  (:require
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]))

(def doc-path "configs/discord")
(def file-path "private/discord.edn")

(def source (io/load-edn file-path))

(defn get-url [channel-name]
  (channel-name source))

(defn download! [db]
  (io/dump-edn! file-path (fs/get-doc db doc-path)))

(defn upload! [db]
  (fs/set! db doc-path source))

(comment
  (require '[firebase :refer [db-dev db-prod]])
  (download! (db-dev))
  (upload! (db-prod))
  (get-url :kotori-qvt)
  )
