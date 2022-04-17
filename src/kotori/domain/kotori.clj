(ns kotori.domain.kotori
  (:require
   [clojure.set :refer [rename-keys]]
   [clojure.spec.alpha :as s]
   [clojure.walk :refer [keywordize-keys]]
   [kotori.lib.firestore :as fs]))

(def coll-name "kotoris")
(defn coll-path [user-id] (str coll-name "/" user-id))

(defrecord Cred [auth-token ct0])
(defrecord Proxy [proxy-host proxy-port proxy-user proxy-pass])
(defrecord Info
  [screen-name user-id ^Cred cred ^Proxy proxy])

(s/def ::auth-token string?)
(s/def ::ct0 string?)
(s/def ::cred (s/keys :req-un [::auth-token ::ct0]))

(s/def ::proxy-host string?)
(s/def ::proxy-port int?)
(s/def ::proxy-user string?)
(s/def ::proxy-pass string?)
(s/def ::proxy
  (s/keys :req-un [::proxy-host ::proxy-port ::proxy-user ::proxy-pass]))

(s/def ::screen-name string?)
(s/def ::user-id string?)
(s/def ::info
  (s/keys :req-un [::screen-name ::user-id ::cred]
          :opt-un [::proxy]))

(defn make-info [screen-name user-id cred-map proxy-map]
  (let [cred  (s/conform ::cred (map->Cred cred-map))
        proxy (s/conform ::proxy (map->Proxy proxy-map))]
    (s/conform ::info (->Info screen-name user-id cred proxy))))

(defn fs->cred
  ([db user-id]
   (let [doc-path (fs/doc-path coll-name user-id)]
     (-> db
         (fs/get-doc doc-path)
         :twitter-auth
         (as-> x (into {} x))
         keywordize-keys
         (rename-keys {:auth_token :auth-token})
         map->Cred
         (s/conform ::cred)))))

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

(defn fs->proxy
  [db user-id]
  (let [doc-path    (fs/doc-path coll-name user-id)
        doc         (fs/get-doc db doc-path)
        proxy-label (keyword (:proxy-label doc))
        proxy-path  "configs/proxies"]
    (-> db
        (fs/get-doc proxy-path)
        proxy-label
        (proxy-fs-http)
        (proxy-port-string->number)
        map->Proxy
        (s/conform ::proxy))))

(defn env->info [db env]
  (let [screen-name (:screen-name env)
        user-id     (:user-id env)
        creds       (or (:twitter-auth env)
                        (fs->cred db user-id))
        proxies     (or (:proxies env)
                        (fs->proxy db user-id))]
    {:screen-name screen-name
     :user-id     user-id
     :creds       creds
     :proxies     proxies}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[firebase :refer [db]])

  (->creds (db) "")
  (->proxies (db) "")
  )
