(ns kotori.lib.firestore
  (:refer-clojure :exclude [set! set get-in assoc! assoc])
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

(defn query-order-by [keyword]
  (fn [q]
    (f/order-by q keyword)))

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

(defn query-start-at [date]
  (fn [q]
    (f/start-at q date)))

(defn query-end-before [date]
  (fn [q]
    (f/end-before q date)))

(defn query-range
  "lower以上upper未満.
  rangeによるフィルタリングはさらに他の条件と合わせて複合クエリがつくれない.
  Firestoreの制約によって複合クエリには同じ方向(asc/desc)のインデックの
  作成が必要. しかしrangeのような一つのフィールドでの両方向は
  それ以外のフィールドとインデックスが貼れない.
  そのためfilter-inによる配列での絞り込みを行う."
  [field lower upper]
  (query-filter-in field (range lower upper)))

;; (defn query-range-date
;;   [field from-data to-date]
;;   (query-filter-in field (range lower upper)))

(def query-one (query-limit 1))

;; xqueryのvectorは末尾から適用されるため
;; 表記のために上から昇順で作成されたリストはreverseで降順にする.
(defn make-xquery [v]
  {:pre [(vector? v)]}
  (apply comp (reverse v)))

(defn get-in [db doc-path ^String field_name]
  (-> db
      (f/doc doc-path)
      .get
      deref
      .getData
      (get field_name)))

(defn get-doc
  [db doc-path]
  (-> db
      (f/doc doc-path)
      .get
      deref
      .getData
      (as-> x (into {} x))
      json/->clj))

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

(defn- assoc
  [db tx doc-path field value]
  (let [data (json/->json value)
        doc  (f/doc db doc-path)]
    (f/assoc tx doc field data)))

(defn update!
  "merge!改良版:
  f/merge!だとドット表記を含むkeyがうまく処理できないため,
  トランザクションの中でmapの各key-valueごとassocを呼ぶように修正.
  mapのkeyはStringで渡すことが必要."
  [db doc-path map]
  (f/transact! db
               (fn [tx]
                 (doseq [[^String k v] map]
                   (assoc db tx doc-path k v)))))

(defn set!
  "与えられたデータをFirestoreに書き込む(merge)."
  [db doc-path map]
  (let [data (json/->json map)]
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

(defn create-batch [db]
  (f/batch db))

(defn commit-batch! [b]
  (f/commit! b))

(defn batch-set! [db batch-docs]
  (let [b (f/batch db)]
    (doseq [{:keys [path data]} batch-docs]
      (set db b path data))
    (f/commit! b)))

(defn doc-exists? [db path]
  "Documentの存在判定はDocumentSnapshotから,
   言い換えれば通信(get)によって実際に取得しないとわからない."
  (let [doc (f/doc db path)]
    (-> doc
        .get
        deref
        .exists)))

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

  ;;;




  )
