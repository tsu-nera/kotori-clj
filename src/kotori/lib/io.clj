(ns kotori.lib.io
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]))

(defn resource [args]
  (io/resource args))

(defn input-stream [args]
  (io/input-stream args))

(defn load-edn [file-path]
  (-> file-path
      resource
      slurp
      edn/read-string))

(defn dump-edn! [file-path data]
  (let [save-data (with-out-str (pprint data))]
    ;; io/resouceはfileが存在しないときはnilを返す.
    ;; fileが存在しない場合は新規作成したいので io/resouceは使わない.
    (-> (str "resources/" file-path)
        (spit save-data))))
