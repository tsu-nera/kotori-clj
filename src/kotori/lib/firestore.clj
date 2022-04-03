(ns kotori.lib.firestore
  (:refer-clojure :exclude [set! set get-in assoc!])
  (:require
   [firestore-clj.core :as f]
   [kotori.lib.json :as json]))

(defn doc-path [coll-path doc-id] (str coll-path "/" doc-id))

(defn query-filter [^String field value]
  (fn [q]
    (f/filter= q field value)))

(defn query-filter-in [^String field arr]
  (fn [q]
    (f/filter-in q field arr)))

(defn query-order-by [& ordering]
  (fn [q]
    (apply f/order-by q ordering)))

(defn query-limit [limit]
  (fn [q]
    (f/limit q limit)))

(defn query-less [field upper]
  (fn [q]
    (f/filter< q field upper)))

(defn query-less= [field upper]
  (fn [q]
    (f/filter<= q field upper)))

(defn query-more [field lower]
  (fn [q]
    (f/filter> q field lower)))

(defn query-more= [field lower]
  (fn [q]
    (f/filter>= q field lower)))

(defn query-range
  "lower以上upper未満.
  rangeによるフィルタリングはさらに他の条件と合わせて複合クエリがつくれない.
  Firestoreの制約によって複合クエリには同じ方向(asc/desc)のインデックの
  作成が必要. しかしrangeのような一つのフィールドでの両方向は
  それ以外のフィールドとインデックスが貼れない.
  そのためfilter-inによる配列での絞り込みを行う."
  [field lower upper]
  (query-filter-in field (range lower upper)))

(def query-one (query-limit 1))

(defn make-xquery [v]
  {:pre [(vector? v)]}
  (apply comp v))

(defn get-in [db doc-path ^String field_name]
  (-> db
      (f/doc doc-path)
      .get
      deref
      .getData
      (get field_name)))

;; 基本的な方針として複雑な条件処理は全てクライアントで実施する.
;; Firestoreの複数フィールドに対するクエリの制限が面倒.
;; せいぜい1000以下にクエリ結果がなるようにxqueryを構築.
(defn get-docs
  ([db coll-path]
   ;; (get-docs db coll-path identity)
   ;; どうせこのルートはデバッグ用なので小さい値を入れておく.
   (get-docs db coll-path (query-limit 3)))
  ([db coll-path xquery]
   (-> db
       (f/coll coll-path)
       xquery
       f/pullv
       ;; ここで通信が発生してデータ取得-> vectorへ(遅延ではない).
       json/->clj)))

(defn assoc!
  "与えられたデータでFirestoreのdocを更新する(1フィールドのみ)"
  [db doc-path field value]
  (let [data (json/->json value)]
    (-> db
        (f/doc doc-path)
        (f/assoc! field data))))

(defn merge!
  "与えられたデータでFirestoreのdocを更新する(複数フィールド)"
  [db doc-path map]
  (let [data (json/->json map)]
    (-> db
        (f/doc doc-path)
        (f/merge! data))))

(defn set!
  "与えられたデータをFirestoreに書き込む(merge)."
  [db doc-path m]
  (let [data (json/->json m)]
    (-> db
        (f/doc doc-path)
        (f/set! data :merge))))

(defn make-batch-docs [id-str path docs]
  (into [] (map (fn [data]
                  (let [id (get data id-str)]
                    {:path (doc-path path id)
                     :data data}))
                docs)))

(defn- set
  "batch setのためのhelper function"
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

  (def queries (make-xquery [q-limit q-order-popular]))

  (def docs (get-docs (db) dmm-path queries))

  (get-in (db) "providers/dmm" "products_crawled_time")
  )
