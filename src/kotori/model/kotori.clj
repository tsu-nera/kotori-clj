(ns kotori.model.kotori
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [clojure.set :refer [rename-keys]]
   [clojure.walk :refer [keywordize-keys]]
   [firestore-clj.core :as fs]
   [integrant.core :as ig]
   [kotori.lib.twitter.private :as private]))

(defn id->coll-path [id]  (str "kotoris/" id))

(defn doc->twitter-auth
  [doc]
  (-> doc
      (:twitter-auth)
      (as-> x (into {} x))
      (keywordize-keys)
      (rename-keys {:auth_token :auth-token})))


(defonce doc nil)
(defonce twitter-auth nil)
(defonce proxies nil)

(defn- proxy-fs-http [m]
  (-> m
      (as-> x (into {} x))
      (rename-keys
       {"ip"       :proxy-host
        "port"     :proxy-port
        "username" :proxy-user
        "password" :proxy-pass})))

(defn proxy-port-string->number [m]
  (let [port (:proxy-port m)]
    (assoc m :proxy-port (Integer. port))))

(defmethod ig/init-key ::db [_ {:keys [config db]}]
  (let [user-id   (:userid config)
        coll-path (id->coll-path user-id)]
    (def doc (-> db
                 (fs/doc coll-path)
                 (.get)
                 (deref)
                 (.getData)
                 (as-> x (into {} x))
                 (keywordize-keys)
                 (as-> x (cske/transform-keys csk/->kebab-case-keyword x))))
    (def twitter-auth (doc->twitter-auth doc))

    (let [proxy-label (:proxy-label doc)
          proxy-path  "configs/proxies"]
      (def proxies (-> db
                       (fs/doc proxy-path)
                       (.get)
                       (deref)
                       (.getData)
                       (get proxy-label)
                       (proxy-fs-http)
                       (proxy-port-string->number)))))
  :initalized)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment

  (require '[integrant.repl.state :refer [config system]])
  (def userid (:userid (:config (::app config))))

  (def doc (-> db
               (fs/doc (id->coll-path userid))))

  @(.get doc)

  (-> db
      (fs/doc "configs/proxies")
      (.get)
      (deref)
      (.getData)
      (get "sakura")
      (proxy-fs-http)
      ;; (as-> x (into {} x))
      ;; (keywordize-keys)
      ;; (rename-keys {"ip" :proxy-host "port" :proxy-port "username" :proxy-user "password" :proxy-pass})
      )

  )

(comment
  doc

  ;; うーん, firestoreとのやりとりで
  ;; いちいちclojure hash-mapと java mapの変換が冗長だな.
  ;; こういうときにマクロを書くんだろうな.
  (def twitter-auth (->> doc
                         (:twitter_auth)
                         (into {} )
                         (keywordize-keys)))
  twitter-auth
  )