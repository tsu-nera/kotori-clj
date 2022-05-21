(ns kotori.lib.io
  (:require
   [clojure.data.csv :as csv]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]))

(defn resource [args]
  (io/resource args))

(defn input-stream [args]
  (io/input-stream args))

(defn exists [file-path]
  (.exists (io/as-file file-path)))

(defn load-edn [file-path]
  (when-let [file (resource file-path)]
    (-> file
        slurp
        edn/read-string)))

(defn dump-edn! [file-path data]
  (let [save-data (with-out-str (pprint data))
        ;; io/resouceはfileが存在しないときはnilを返す.
        ;; fileが存在しない場合は新規作成したいので io/resouceは使わない.
        save-path (str "resources/" file-path)]
    (doto save-path
      io/make-parents  ;; ディレクトリが存在しないならば新規作成
      (spit save-data))))

(defn dump-example-edn! [file-path data]
  (let [save-data (with-out-str (pprint data))
        save-path (str "examples/" file-path)]
    (doto save-path
      io/make-parents
      (spit save-data))))

(defn dump-str! [file-path data]
  (spit file-path data))

(defn csv-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data) ;; First row is the header
            (map keyword) ;; Drop if you want string keys instead
            repeat)
       (rest csv-data)))

(defn maps->csv-data
  "Takes a collection of maps and returns csv-data
   (vector of vectors with all values)."
  [maps]
  (let [columns (-> maps first keys)
        headers (mapv name columns)
        rows    (mapv #(mapv % columns) maps)]
    (into [headers] rows)))

(defn dump-csv! [file-path vec-of-vecs]
  (with-open [writer (io/writer file-path)]
    (csv/write-csv writer vec-of-vecs)))

(defn dump-csv-from-maps!
  "Takes a file (path, name and extension) and a collection of maps
   transforms data (vector of vectors with all values)
   writes csv file."
  [file-path maps]
  (->> maps maps->csv-data (dump-csv! file-path)))
