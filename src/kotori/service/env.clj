(ns kotori.service.env
  (:require
   [integrant.core :as ig]
   [kotori.lib.io :as io]))

(defmethod ig/init-key ::env [_ {:keys [path]}]
  (-> path
      io/load-edn))

(defmethod ig/init-key ::proxies [_ {:keys [path]}]
  (-> path
      io/load-edn))
