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

  (f/doc (db) "experiments/test")

  ;; cljure.core set!と競合
  (kotori.lib.firestore/set! (db) path m-clj)
  )
