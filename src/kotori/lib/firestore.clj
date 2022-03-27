(ns kotori.lib.firestore
  (:refer-clojure :exclude [set! set])
  (:require
   [firestore-clj.core :as f]
   [kotori.domain.dmm.product :as product]
   [kotori.lib.json :as json]))

(defn set!
  [db path m]
  (let [data (json/->json m)]
    (-> db
        (f/doc path)
        (f/set! data))))

(defn make-batch-docs [id-str path docs]
  (into [] (map (fn [data]
                  (let [id (get data id-str)]
                    {:path (str path id)
                     :data data}))
                docs)))

(defn- set
  [db b path m]
  (let [data (json/->json m)
        doc  (f/doc db path)]
    (f/set b doc data)))

(defn batch-set! [db batch-docs]
  (let [b (f/batch db)]
    (doseq [{:keys [path data]} batch-docs]
      (set db b path data))
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
