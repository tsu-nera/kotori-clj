(ns kotori.lib.firestore
  (:refer-clojure :exclude [set! set get-in assoc! assoc])
  (:require
   [clojure.string :as string]
   [firestore-clj.core :as f]
   [kotori.lib.json :as json]
   [kotori.lib.time :as t]))

(defn doc-path [coll-path doc-id] (str coll-path "/" doc-id))

;; xqueryのvectorは末尾から適用されるため
;; 表記のために上から昇順で作成されたリストはreverseで降順にする.
(defn make-xquery [v]
  {:pre [(vector? v)]}
  (apply comp (reverse v)))

(defn query-filter [^String field value]
  (fn [q]
    (f/filter= q field value)))

(defn query-filter-in [^String field arr]
  (fn [q]
    (f/filter-in q field arr)))

(defn query-filter-not
  "firestore-cljがwhereNotEqualTo未サポートのため"
  ([q field value]
   (.whereNotEqualTo q field value)))

;; どうもkeywordで暗黙の昇順の並び替えが入っている気がする...
(defn query-exists
  ([^String keyword]
   (fn [q]
     (query-filter-not q keyword false))))

;; 結果のcollにソートが入ることに注意. それが嫌ならquery-existsを使う.
(defn query-exists-by-order
  "Fieldの存在判定にorder-byが利用できる. nullは除外される."
  ([keyword]
   (fn [q]
     (f/order-by q keyword)))
  ([keyword ordering]
   ;; ordering = :asc or :desc
   (fn [q]
     (f/order-by q keyword ordering))))

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

(defn query-between [days-ago days field]
  (let [base-time (t/date->days-ago days-ago)
        from-time (t/->fs-timestamp base-time)
        to-time   (t/->fs-timestamp (t/date->days-later days base-time))]
    (make-xquery [(query-order-by field)
                  (query-start-at from-time)
                  (query-end-before to-time)])))

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

(defn get-in "
  ドット表記のネスト構造には対応していないので
  ネスト内部の値が必要な場合はマップを呼び出し元で処理すること."
  [db doc-path ^String field_name]
  (-> db
      (f/doc doc-path)
      .get
      deref
      .getData
      (as-> x (into {} x))
      json/->clj
      (get field_name)))

(defn get-doc
  ([db doc-path]
   (-> db
       (f/doc doc-path)
       .get
       deref
       .getData
       (as-> x (into {} x))
       json/->clj))
  ([db coll-path doc-id]
   (get-doc db (doc-path coll-path doc-id))))

(defn get-doc-raw
  ([db doc-path]
   (-> db
       (f/doc doc-path)
       .get
       deref
       .getData
       (as-> x (into {} x))))
  ([db coll-path doc-id]
   (get-doc db (doc-path coll-path doc-id))))

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
       f/pullv                                        ; f/pullv
       ;; ここで通信が発生してデータ取得-> vectorへ(遅延ではない).
       ;; ここでdoc.idの情報は失うことに注意.
       ;; id情報も一緒に取得ならばf/pullでMapが帰る.
       json/->clj)))

(defn get-docs-by-ids
  ([db coll-path doc-ids]
   (-> db
       (f/coll coll-path)
       (f/docs doc-ids)
       f/pull-docs
       json/->clj)))

(defn get-coll-ids
  [db coll-path]
  (-> db
      (f/coll coll-path)
      (.listDocuments)
      (as-> x (map #(.getId %) x))))

(defn get-id-doc-map
  ([db coll-path]
   (get-id-doc-map db coll-path identity))
  ([db coll-path xquery]
   (-> db
       (f/coll coll-path)
       xquery
       f/pull
       (as-> x (reduce-kv (fn [m k v]
                            (clojure.core/assoc
                             m k (json/->clj v))) {} x)))))

(defn get-filter-docs
  ([db coll-path filter-map]
   (-> db
       (f/coll coll-path)
       (f/filter= filter-map)
       f/pullv
       json/->clj)))

(defn get-docs-with-assoc-id
  [db coll-path]
  (let [doc-map (get-id-doc-map db coll-path)]
    (->> doc-map
         (map (juxt key val))
         (map (fn [[k v]] (clojure.core/assoc v :id k))))))

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

(defn make-nested-key
  "firestoreのネストしたfieldをupdateするときはdot表記のキーを指定"
  [field-list]
  (string/join "." field-list))

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

(defn overwrite!
  "与えられたデータをFirestoreに書き込む"
  [db doc-path map]
  (let [data (json/->json map)]
    (-> db
        (f/doc doc-path)
        (f/set! data))))

(defn add!
  "与えられたデータをFirestoreに書き込む. IDは自動採番."
  [db coll-path map]
  (let [data (json/->json map)]
    (-> db
        (f/coll coll-path)
        (f/add! data))))

(defn set!
  "与えられたデータをFirestoreに書き込む(merge)."
  [db doc-path map]
  (let [data (json/->json map)]
    (-> db
        (f/doc doc-path)
        (f/set! data :merge))))

(defn set-raw!
  [db doc-path map]
  (-> db
      (f/doc doc-path)
      (f/set! map :merge)))

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

(defn batch-add!
  "バッチ書き込みはaddをサポートしていない(set,udpate,deleteのみ).
  この 関数ではデータの数だけadd!をして結果をvectorで返すのみ.
  mapは遅延シーケンスを返すのでintoで評価を強制しないといけない."
  [db coll-path docs]
  (into [] (map
            #(add! db coll-path (json/->json %))
            docs)))

;; Documentの存在判定はDocumentSnapshotから.
;; 言い換えれば通信(get)によって実際に取得しないとわからない.
(defn doc-exists? [db path]
  (let [doc (f/doc db path)]
    (-> doc
        .get
        deref
        .exists)))

(defn doc-field-exists?
  "指定したpathの指定したfieldが存在するかチェック.
  存在しないpathが指定されると .getDataがnilになりcontains?はfalse.
  "
  [db path field]
  (let [doc (f/doc db path)]
    (-> doc
        .get
        deref
        .getData
        (contains? field))))

(defn coll->id-map
  ([db coll-path]
   (coll->id-map db coll-path :id))
  ([db coll-path id-key]
   (let [docs (get-docs-with-assoc-id db coll-path)]
     (reduce
      (fn [acc doc]
        (clojure.core/assoc
         acc (keyword (get doc id-key)) doc)) {} docs))))

(defn delete!
  [db doc-path]
  (-> db
      (f/doc doc-path)
      (f/delete!)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[firebase :refer [db db-prod]])

  (def coll-path "experiments")
  (def dmm-path "providers/dmm/products")

  (def q-limit (query-limit 5))
  (def id-doc-map (get-id-doc-map (db) dmm-path))

  (reduce-kv (fn [m k v]
               (clojure.core/assoc m k (json/->clj v))) {} id-doc-map)

  (doc-field-exists? (db) "providers/dmmmm" "foo")

  (def resp (get-docs-by-ids (db-prod) "providers/dmm/products"
                             ["idbd00841" "miaa00573"]))
 ;;;
  )
