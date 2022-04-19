(ns kotori.domain.source.core
  (:require
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]
   [kotori.lib.time :as time]))

(def coll-path "sources")
(def dir-path coll-path)
(def info-path (str dir-path "/" "info.edn"))

(defn ->source-path [source-id]
  (str coll-path "/" source-id))

(defn ->items-path [source-id]
  (str (->source-path source-id) "/items"))

(defn ->source [label]
  (let [file-path (str dir-path "/" label ".edn")]
    (io/load-edn file-path)))

(defn ->info [label]
  (let [info (io/load-edn info-path)]
    (get info label)))

(defn- register-info [db label ts]
  (let [info        {:label label :created-at ts :updated-at ts}
        source-id   (-> (fs/add! db coll-path info)
                        .getId)
        source-path (->source-path source-id)]
    (fs/assoc! db source-path "id" source-id)
    source-id))

(defn- register-items! [db source-id items]
  (let [items-path (->items-path source-id)]
    (fs/batch-add! db items-path items)))

(defn register!
  "テキストの配列からソースをFirestoreに登録する.
  1. Firestore /sourcesにdocを新規作成. doc-idは自動生成.
  2. /sources/{id}/itemsにdatasetsを格納. dataごとにidを自動生成.
  3. source-idとdata-idを取得してローカルファイルに保存."
  [db label items]
  (let [ts        (time/fs-now)
        source-id (register-info db label ts)]
    (doto db
      (register-items! source-id items)
      (download-info!)
      (download-items! label))))

(defn upload! [db label]
  (let [source      (->source label)
        source-id   (:id (->info label))
        source-path (->source-path source-id)
        items-path  (->items-path source-id)]
    (map (fn [data]
           (let [doc-path (str items-path "/" (:id data))]
             (fs/set! db doc-path data)))
         source)
    (fs/assoc! db source-path "update_time" (time/fs-now))))

(defn download-info! [db]
  (let [docs (fs/get-docs db coll-path)
        data (reduce
              (fn [acc doc]
                (assoc acc (:label doc) doc)) {} docs)]
    (io/dump-edn! info-path data)))

(defn download-items!
  ([db label]
   (download-items! db label (str "sources/" label ".edn")))
  ([db label file-path]
   (let [info       (->info label)
         items-path (->items-path (:id info))
         docs       (fs/get-docs-with-assoc-id db items-path)
         data       (reduce
                     (fn [acc doc]
                       (conj acc doc)) [] docs)]
     (io/dump-edn! file-path data))))

;; (defn download-all!
;;   [db]
;;   (doto db
;;     (meigen/download!)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[firebase :refer [db]])

  (def datasets ["ぶぁーか" "かーば" "ブタのけつ"])
  (def items (map (fn [text] {:text text}) datasets))

  (def source-id (register-info (db) "test" (time/fs-now)))
  (def result (register-items! (db) source-id items))

  (download-info! (db))
  (->info "test")
  (download-items! (db) "test")
  (register! (db) "test" items)
  )

(comment
  (require '[firebase :refer [db]])

  (download-info! (db))

  ;; loadするとtimstampはinstantになる.
  (io/load-edn info-path)
  )
