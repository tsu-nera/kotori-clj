(ns kotori.domain.kotori.strategy
  (:require
   [clojure.spec.alpha :as s]))

(defrecord Strategy [floor-code genre-id])

(s/def ::floor-code (s/nilable string?))
(s/def ::genre-id (s/nilable int?))

(s/def ::strategy
  (s/keys :opt-un [::floor-code ::genre-id]))

(defn create [config]
  (s/conform ::strategy (map->Strategy config)))
