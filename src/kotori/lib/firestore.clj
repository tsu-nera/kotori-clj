(ns kotori.lib.firestore
  (:refer-clojure :exclude [set!])
  (:require
   [firestore-clj.core :as f]
   [kotori.lib.json :as json]))

(defn set!
  [db path m]
  (let [data (json/->json m)]
    (-> db
        (f/doc path)
        (f/set! data))))

(defn set
  [db b path m]
  (let [data (json/->json m)
        doc  (f/doc db path)]
    (f/set b doc data)))

#_(defn batch-set! "
  firestroreのbatch writeの仕様で一回の書き込みは500まで.
  そのため500単位でchunkごとに書き込む.
  また Fieldに対するincや配列への追加も1つの書き込みとなる.
  "
    [db coll-path doc-ids doc-data-list]
    (let [b (f/batch db)]
      (-> docs
          (json/->json)
          (set-fn))
      (f/commit! b)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(comment
  (require '[local :refer [db]])

  (def path "experiments/test")

  (def m-clj
    {:a    1
     :b    2
     :c    {:d 1 :e 2 "f" 3}
     "a.h" 3
     :g_h  1})

  ;; cljure.core set!と競合
  (def result (kotori.lib.firestore/set! (db) path m-clj))

  (tap> result)
  )
