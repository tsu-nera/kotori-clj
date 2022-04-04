(ns kotori.domain.kotori
  (:require
   [clojure.set :refer [rename-keys]]
   [clojure.walk :refer [keywordize-keys]]
   [kotori.lib.firestore :as fs]))

(def coll-name "kotoris")
(defn coll-path [user-id] (str coll-name "/" user-id))

(defn ->creds
  ([db user-id]
   (let [doc-path (fs/doc-path coll-name user-id)]
     (-> db
         (fs/get-doc doc-path)
         :twitter-auth
         (as-> x (into {} x))
         keywordize-keys
         (rename-keys {:auth_token :auth-token})))))

(defn- proxy-fs-http [m]
  (-> m
      (as-> x (into {} x))
      (rename-keys
       {"ip"       :proxy-host
        "port"     :proxy-port
        "username" :proxy-user
        "password" :proxy-pass})))

(defn- proxy-port-string->number [m]
  (let [port (:proxy-port m)]
    (assoc m :proxy-port (Integer. port))))

(defn ->proxies
  [db user-id]
  (let [doc-path    (fs/doc-path coll-name user-id)
        doc         (fs/get-doc db doc-path)
        proxy-label (keyword (:proxy-label doc))
        proxy-path  "configs/proxies"]
    (-> db
        (fs/get-doc proxy-path)
        proxy-label
        (proxy-fs-http)
        (proxy-port-string->number))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[firebase :refer [db]])

  (->creds (db) "")
  (->proxies (db) "")
  )
