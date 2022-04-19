(ns kotori.domain.source.core
  (:require
   [kotori.lib.io :as io]))

(def coll-path "sources")
(def dir-path coll-path)
(def info-path (str dir-path "/" "info.edn"))

(defn ->source [label]
  (let [file-path (str dir-path "/" label ".edn")]
    (io/load-edn file-path)))

(defn ->source-path [source-id]
  (str coll-path "/" source-id))

(defn ->info [label]
  (let [info (io/load-edn info-path)]
    (get info label)))

(defn ->items-path [source-id]
  (str (->source-path source-id) "/items"))
