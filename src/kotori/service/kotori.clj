(ns kotori.service.kotori
  (:require
   [integrant.core :as ig]
   [kotori.lib.io :as io]))

(defmethod ig/init-key ::by-ids [_ {:keys [path]}]
  (let [config (-> path
                   io/load-edn)]
    config))

(defmethod ig/init-key ::by-codes [_ config]
  (->> config
       vals
       (map (juxt :code
                  identity))
       (map (fn [[code data]]
              {code data}))
       (reduce conj)))

(defmethod ig/init-key ::by-names [_ config]
  (->> config
       vals
       (map (juxt :screen-name
                  identity))
       (map (fn [[screen-name data]]
              {screen-name data}))
       (reduce conj)))
