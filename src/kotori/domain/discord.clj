(ns kotori.domain.discord
  (:require
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]
   [kotori.lib.json :as json]))

(def doc-path "configs/discord")
(def file-path "private/discord.edn")
(def source (io/load-edn file-path))

(defn get-url [channel-name]
  (channel-name source))

(defn download! [db]
  (let [data (fs/get-in db doc-path "channels")]
    (->> data
         (into {})
         (json/->clj)
         (io/dump-edn! file-path))))

(comment
  (require '[firebase :refer [db-prod]])
  (download! (db-prod))

  (get-url :kotori-qvt)
  )
