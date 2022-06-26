(ns kotori.service.kotori
  (:require
   [integrant.core :as ig]
   [kotori.lib.io :as io]))

(defmethod ig/init-key ::strategies [_ {:keys [path]}]
  (-> path io/load-edn))

(defmethod ig/init-key ::config [_ {:keys [path]}]
  (-> path io/load-edn))

(defmethod ig/init-key ::ids [_ apps]
  (->> apps
       vals
       (map (juxt :user-id
                  identity))
       (map (fn [[user-id data]]
              {user-id data}))
       (reduce conj)))

(defmethod ig/init-key ::names [_ apps]
  (->> apps
       vals
       (map (juxt :screen-name
                  identity))
       (map (fn [[screen-name data]]
              {screen-name data}))
       (reduce conj)))

(defn- assoc-if-proxy [proxies]
  (fn [m k v]
    (let [label (:proxy-label v)
          new-v (if label
                  (assoc v :proxy (label proxies))
                  (assoc v :proxy {}))]
      (assoc m k new-v))))

(defn- assoc-if-strategy [strategies]
  (fn [m code v]
    (let [strategy (get strategies code)]
      (assoc m code (cond-> v
                      strategy
                      (assoc :strategy strategy))))))

(defmethod ig/init-key ::apps [_ {:keys [config strategies proxies]}]
  (->> config
       (reduce-kv (assoc-if-strategy strategies) {})
       (reduce-kv (assoc-if-proxy proxies) {})))

(defmethod ig/init-key ::codes [_ apps]
  apps)
