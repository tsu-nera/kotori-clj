(ns kotori.lib.io
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

(defn resource [args]
  (io/resource args))

(defn input-stream [args]
  (io/input-stream args))

(defn load-edn [file-path]
  (-> file-path
      resource
      slurp
      edn/read-string))
