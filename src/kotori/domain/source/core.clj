(ns kotori.domain.source.core
  (:require
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]
   [kotori.lib.time :as time]))

(def coll-path "sources")

(defn ->source-path [source-id]
  (str coll-path "/" source-id))
(defn ->items-path [source-id]
  (str (->source-path source-id) "/items"))

(def dir-path "sources")
(def info-path (str dir-path "/" "info.edn"))

(defn ->source [name]
  (let [file-path (str dir-path "/" name ".edn")]
    (io/load-edn file-path)))

(defn ->info [name]
  (let [info (first (io/load-edn info-path))]
    (get info name)))

(defn upload! [db name]
  (let [source      (->source name)
        source-id   (:id (->info name))
        source-path (->source-path source-id)
        items-path  (->items-path source-id)]
    (map (fn [data]
           (let [doc-path (str items-path "/" (:id data))]
             (fs/set! db doc-path data)))
         source)
    (fs/assoc! db source-path "update_time" (time/fs-now))))

(defn download-info! [db]
  (let [docs    (fs/get-docs db coll-path)
        sources (map (fn [m]
                       {(:label m) m})  docs)]
    (io/dump-edn! info-path sources)))

;; (defn download-all!
;;   [db]
;;   (doto db
;;     (meigen/download!)))

(comment
  (require '[firebase :refer [db]])

  (download-info! (db))

  ;; loadするとtimstampはinstantになる.
  (io/load-edn info-path)
  )
