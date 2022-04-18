(ns kotori.domain.source.meigen
  (:require
   [kotori.domain.source.core :as core]
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]))

(def coll-path "sources/source_0001/meigens")
(def file-path "sources/meigen.edn")

(def label "meigen")
(def info (core/->info label))

(def source (core/->source label))

#_(defn pick-random
    ([]
     (rand-nth source))
    ([db]
     (let [coll-ids (fs/get-coll-ids db coll-path)
           doc-id   (rand-nth coll-ids)]
       (fs/get-doc db coll-path doc-id))))

(defn build-text [data]
  (let [{content :content, author :author} data]
    (str content "\n\n" author)))

(defn download! [db]
  (let [docs (fs/get-docs-with-assoc-id db coll-path)]
    (io/dump-edn! file-path docs)))

(comment
  ;;;
  (require '[firebase :refer [db-dev]])

  (def doc-id (rand-nth (fs/get-coll-ids (db-dev) coll-path)))
  (fs/get-doc (db-dev) coll-path doc-id)

  ;;;
  (download! (db-dev))

  (core/upload! (db-dev) label)
  ;;;
  )
