(ns tools.source
  (:require
   [firebase :refer [db db-dev db-prod]]
   [kotori.domain.source.core :as src]
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]
   [kotori.lib.time :as time]
   [tools.item :as item]))

(defn- register-info [db label ts]
  (let [info        {:label label :created-time ts :updated-time ts}
        source-id   (-> (fs/add! db src/coll-path info)
                        .getId)
        source-path (src/->source-path source-id)]
    (fs/assoc! db source-path "id" source-id)
    source-id))

(defn- register-items! [db source-id items]
  (let [items-path (src/->items-path source-id)]
    (fs/batch-add! db items-path items)))

(defn download-info! [db]
  (let [id-map (fs/coll->id-map db src/coll-path :label)]
    (io/dump-edn! src/info-path id-map)))

(defn download-items!
  ([db label]
   (download-items! db label (str "sources/" label ".edn")))
  ([db label file-path]
   (let [info       (src/->info (keyword label))
         items-path (src/->items-path (:id info))
         docs       (fs/get-docs-with-assoc-id db items-path)
         data       (reduce
                     (fn [acc doc]
                       (conj acc doc)) [] docs)]
     (io/dump-edn! file-path data))))

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
  (let [source      (src/->source label)
        info        (src/->info label)
        source-id   (:id info)
        source-path (src/->source-path source-id)
        items-path  (src/->items-path source-id)]
    (fs/set! db source-path
             (assoc info :updated-time (time/fs-now)))
    (map (fn [data]
           (let [doc-path (str items-path "/" (:id data))]
             (fs/set! db doc-path data)))
         source)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (register! (db-dev) "qvt_0003" (item/->items item/qvt-0003))

  (upload! (db-prod) "meigen")
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (def source-id (register-info (db) "test" (time/fs-now)))
  (def result (register-items! (db) source-id item/qvt-dev))

  (download-info! (db))
  (src/->info "qvt_dev")
  (download-items! (db) "test")
  (register! (db) "test" item/qvt-dev)
  )

(comment
  (download-info! (db))

  ;; loadするとtimstampはinstantになる.
  (io/load-edn src/info-path)
  )
