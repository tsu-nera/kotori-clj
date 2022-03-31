(ns kotori.lib.firestore
  (:refer-clojure :exclude [set! set])
  (:require
   [firestore-clj.core :as f]
   [kotori.lib.json :as json]))

(defn doc-path [coll-path doc-id] (str coll-path "/" doc-id))

(defn query-filter-in [query-str]
  (fn [q]
    (f/limit q query-str)))

(defn query-order-by [& ordering]
  (fn [q]
    (apply f/order-by q ordering)))

(defn query-limit [limit]
  (fn [q]
    (f/limit q limit)))

(def query-one (query-limit 1))

(defn make-queries [v]
  {:pre [(vector? v)]}
  (apply comp v))

(defn get-docs
  ([db coll-path]
   ;; (get-docs db coll-path identity)
   (get-docs db coll-path (query-limit 5)))
  ([db coll-path queries]
   (-> db
       (f/coll coll-path)
       queries
       f/pullv
       json/->clj)))

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

  (def q-limit (query-limit 5))
  (def q-order-popular
    (query-order-by "last_crawled_time" :desc
                    "rank_popular" :asc))

  (def queries (make-queries [q-limit q-order-popular]))

  (def docs (get-docs (db) dmm-path queries))
  )
