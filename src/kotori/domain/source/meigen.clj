(ns kotori.domain.source.meigen
  (:require
   [clojure.walk :refer [keywordize-keys]]
   [firestore-clj.core :as f]
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]
   [kotori.procedure.kotori :as kotori]))

(def coll-path "sources/source_0001/meigens")
(def file-path "sources/meigen.edn")

(def source (io/load-edn file-path))

(defn pick-random
  ([]
   (rand-nth source))
  ([db]
   (let [coll-ids (fs/get-coll-ids db coll-path)
         doc-id   (rand-nth coll-ids)]
     (fs/get-doc db coll-path doc-id))))

(defn make-tweet-text []
  (let [{content :content, author :author} (pick-random)]
    (str content "\n\n" author)))

(comment
  ;;;
  (require '[firebase :refer [db-dev]])
  ;;;
  (pick-random)
  (pick-random (db-dev))

  (make-tweet-text)
  ;;;
  (def doc-id (rand-nth (fs/get-coll-ids (db-dev) coll-path)))
  (fs/get-doc (db-dev) coll-path doc-id)
  ;;;
  )

