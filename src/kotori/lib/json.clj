(ns kotori.lib.json
  "Clojure Map <-> Json Proxy Library"
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]))

(defn ->json-keyword
  "Clojure Map into JSON i.e. kebab-case->snake_case."
  [clojure-map]
  (cske/transform-keys csk/->snake_case clojure-map))

(defn ->json
  "Clojure Map into JSON i.e. keyword->string, kebab-case->snake_case."
  [clojure-map]
  (cske/transform-keys csk/->snake_case_string clojure-map))

(defn ->clj
  "JSON into Clojure Map i.e. string->keyword, snake_case->kebab-case"
  [json-map]
  (cske/transform-keys csk/->kebab-case-keyword json-map))
