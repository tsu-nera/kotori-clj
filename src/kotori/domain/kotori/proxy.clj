(ns kotori.domain.kotori.proxy
  (:require
   [clojure.spec.alpha :as s]))

(defrecord Info [proxy-host
                 proxy-port
                 proxy-user
                 proxy-pass])

(s/def ::proxy-host string?)
(s/def ::proxy-port int?)
(s/def ::proxy-user string?)
(s/def ::proxy-pass string?)

(s/def ::info
  (s/keys :opt-un [::proxy-host ::proxy-port
                   ::proxy-user ::proxy-pass]))
