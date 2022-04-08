(ns kotori.lib.io
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

(defn resource [n]
  (io/resource n))

(defn load-edn [file-path]
  (-> file-path
      io/resource
      slurp
      edn/read-string))
