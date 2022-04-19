(ns kotori.domain.source.meigen
  (:require
   [kotori.domain.source.core :as core]))

(def label "meigen")
(def file-path "sources/meigen.edn")

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

(comment
  ;;;
  (require '[firebase :refer [db-dev]])

  (core/upload! (db-dev) label)
  ;;;
  )
