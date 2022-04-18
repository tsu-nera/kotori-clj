(ns kotori.service.kotori
  (:require
   [integrant.core :as ig]
   [kotori.lib.io :as io]))

(defn assoc-proxy [proxies]
  (fn [m k v]
    (let [label (:proxy-label v)
          new-v (if label
                  (assoc v :proxy (label proxies))
                  (assoc v :proxy {}))]
      (assoc m k new-v))))

(defmethod ig/init-key ::by-ids [_ {:keys [path proxies]}]
  (let [config   (-> path
                     io/load-edn)
        assoc-fn (assoc-proxy proxies)]
    (reduce-kv assoc-fn {} config)))

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
