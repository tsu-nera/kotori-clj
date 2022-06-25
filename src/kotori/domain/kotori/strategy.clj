(ns kotori.domain.kotori.strategy
  (:require
   [clojure.spec.alpha :as s]))

(defrecord Strategy [genre-id])

(s/def ::genre-id (s/nilable int?))

(s/def ::strategy
  (s/keys :opt-un [::genre-id]))
