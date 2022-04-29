(ns kotori.domain.config.discord
  (:require
   [clojure.walk :refer [stringify-keys]]
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]))

(def doc-path "configs/discord")
(def file-path "private/discord.edn")

(def source (io/load-edn file-path))

(defn get-url [channel-name]
  (channel-name source))

(defn download! [db]
  (->> doc-path
       (fs/get-doc db)
       (io/dump-edn! file-path)))

(defn upload! [db]
  (->> source
       stringify-keys
       (fs/set-raw! db doc-path)))

(comment
  (require '[firebase :refer [db-dev db-prod]])
  (download! (db-dev))
  (upload! (db-prod))
  (get-url :kotori-qvt)
  )
