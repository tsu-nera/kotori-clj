(ns kotori.lib.firestore
  (:refer-clojure :exclude [set! set])
  (:require
   [firestore-clj.core :as f]
   [kotori.lib.json :as json]))

(defn doc-path [coll-path doc-id] (str coll-path "/" doc-id))

(defn query-limit [limit]
  (fn [q]
    (f/limit q limit)))

(defn get-docs
  ([db coll-path]
   (get-docs db coll-path identity))
  ([db coll-path queries]
   (-> (f/coll db coll-path)
       queries
       f/pullv
       (json/->clj))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn set!
  [db path m]
  (let [data (json/->json m)]
    (-> db
        (f/doc path)
        (f/set! data))))

(defn make-batch-docs [id-str path docs]
  (into [] (map (fn [data]
                  (let [id (get data id-str)]
                    {:path (doc-path path id)
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
  (require '[devtools :refer [db]])

  (def coll-path "experiments")
  (def dmm-path "providers/dmm/products")
  (def doc-path "experiments/test")

  (def m-clj
    {:a    1
     :b    2
     :c    {:d 1 :e 2 "f" 3}
     "a.h" 3
     :g_h  1})

  ;; cljure.core set!と競合
  (def result (kotori.lib.firestore/set! (db) doc-path m-clj))

  (tap> result)
  ;;;;;;;;;;;;;;;;;;;;;;;;;

  (defn get-docs
    ([db coll-path]
     (get-docs db coll-path identity))
    ([db coll-path queries]
     (-> (f/coll db coll-path)
         queries
         f/pullv
         (json/->clj))))

  (defn query-limit [limit]
    (fn [q]
      (f/limit q limit)))

  (def query (query-limit 5))

  (-> (f/coll (db) dmm-path)
      f/pullv
      json/->clj)

  (def docs (get-docs (db) dmm-path))
  (count (into [] docs))
  )
