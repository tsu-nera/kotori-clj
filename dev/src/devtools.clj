(ns devtools
  "REPLからの利用を想定したツール."
  (:refer-clojure :exclude [proxy])
  (:require
   [integrant.repl.state :refer [config system]]
   [kotori.procedure.kotori.core :refer [make-info]]
   [kotori.service.firebase :refer [get-db]]
   [twitter-clj.guest :as guest]
   [twitter-clj.private :as private]))

(defn db []
  (get-db))

(defn env []
  (get system :kotori.service.env/env))

(defn dev? []
  (= (:env (env)) :development))

(defn prod? []
  (= (:env (env)) :production))

(defn get-tweet-guest [id]
  (guest/get-tweet id))

(defn twitter-auth []
  (-> system
      (get :kotori.service.env/env)
      :twitter-auth))

(defn get-tweet-private
  ([id]
   (private/get-tweet (twitter-auth) id)))

(defn dmm-creds []
  (-> system
      (get :kotori.service.env/env)
      (select-keys [:api-id :affiliate-id])))

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

(defn kotori-info-by-name [screen-name]
  (make-info (kotori-by-name screen-name)))

(defn kotori-info [code]
  (make-info (kotori-by-code code)))

(def info-dev (delay (kotori-info "0003")))

(defn ->screen-name [code]
  (:screen-name (kotori-info code)))

(defn ->user-id [code]
  (:user-id (kotori-info code)))

(defn get-tweet-with-info
  ([code id]
   (let [creds (:creds (kotori-info code))]
     (private/get-tweet creds (str id)))))
#_(get-tweet-with-info "0001" "")
