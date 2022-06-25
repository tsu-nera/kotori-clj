(ns kotori.domain.kotori.proxy
  (:require
   [clojure.spec.alpha :as s]))

(defrecord ProxyInfo [proxy-host
                      proxy-port
                      proxy-user
                      proxy-pass])

(s/def ::proxy-host string?)
(s/def ::proxy-port int?)
(s/def ::proxy-user string?)
(s/def ::proxy-pass string?)

(s/def ::proxy-info
  (s/keys :opt-un [::proxy-host ::proxy-port
                   ::proxy-user ::proxy-pass]))

(defn create [config]
  (let [test-proxy (s/conform ::proxy-info
                              (map->ProxyInfo config))]
    (if-not (s/invalid? test-proxy) test-proxy {})))
