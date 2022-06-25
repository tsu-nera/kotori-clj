(ns devtools
  "REPLからの利用を想定したツール."
  (:refer-clojure :exclude [proxy])
  (:require
   [integrant.repl.state :refer [config system]]
   [kotori.procedure.kotori.core :refer [config->kotori]]
   [kotori.service.firebase :refer [get-db]]
   [twitter-clj.private :as private]))

(defn db []
  (get-db))

(defn env []
  (get system :kotori.service.env/env))

(defn dev? []
  (= (:env (env)) :development))

(defn prod? []
  (= (:env (env)) :production))

(defn twitter-auth []
  (-> system
      (get :kotori.service.env/env)
      :twitter-auth))

(defn proxies []
  (-> system
      :kotori.service.env/proxies))

(defn proxy [label]
  (get (proxies) label))

(defn kotori-ids []
  (-> system
      :kotori.service.kotori/by-ids))

(defn kotori-names []
  (-> system
      :kotori.service.kotori/by-names))

(defn kotori-codes []
  (-> system
      :kotori.service.kotori/by-codes))

(defn kotori-by-id [user-id]
  (let [key (keyword user-id)]
    (-> (kotori-ids)
        key)))

(defn kotori-by-name [screen-name]
  (-> (kotori-names)
      (get screen-name)))

(defn kotori-by-code [code]
  (-> (kotori-codes)
      (get code)))

(defn code->kotori-by-name [screen-name]
  (config->kotori (kotori-by-name screen-name)))

(defn code->kotori [code]
  (config->kotori (kotori-by-code code)))

(defn kotori-params [db code]
  (let [info (config->kotori (kotori-by-code code))]
    {:db db :info info}))

(def info-dev (delay (code->kotori "0003")))

(defn ->screen-name [code]
  (:screen-name (code->kotori code)))

(defn ->user-id [code]
  (:user-id (code->kotori code)))

(defn get-tweet-with-info
  ([code id]
   (let [creds (:creds (code->kotori code))]
     (private/get-tweet creds (str id)))))
#_(get-tweet-with-info "0001" "")
