(ns user)

(defn dev []
  (require 'dev)
  (in-ns 'dev)
  (println ":switch to the development namespace")
  :loaded)
