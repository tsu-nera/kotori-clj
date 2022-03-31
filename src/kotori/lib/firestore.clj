(ns kotori.lib.firestore
  (:refer-clojure :exclude [set! set])
  (:require
   [firestore-clj.core :as f]
   [kotori.lib.json :as json]))

(defn doc-path [coll-path doc-id] (str coll-path "/" doc-id))

(defn query-filter-in [query-str]
  (fn [q]
    (f/limit q query-str)))

(defn query-limit [limit]
  (fn [q]
    (f/limit q limit)))

(def query-one (query-limit 1))

(defn get-docs
  ([db coll-path]
   (get-docs db coll-path identity))
  ([db coll-path queries]
   (-> db
       (f/coll coll-path)
       queries
       f/pullv
       json/->clj)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn set!
  "与えられたデータをFirestoreに書き込む.
  すでにドキュメントが存在している場合は存在しないフィールドのみ書き込む.
  存在しているフィールドに対してはなにもしない."
  [db path m]
  (let [data (json/->json m)]
    (-> db
        (f/doc path)
        (f/set! data :merge))))

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
    (f/set b doc data :merge)))

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
      json/->clj
      )

  (def coll-ref (f/coll (db) dmm-path))

  (def doc-refs (.listDocuments coll-ref))

  (take 20 doc-refs)



  (.get doc-refs)

  (def docs (get-docs (db) dmm-path))
  (count (into [] docs))
  )
