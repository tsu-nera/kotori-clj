(ns kotori.model.kotori
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [clojure.set :refer [rename-keys]]
   [clojure.walk :refer [keywordize-keys]]
   [firestore-clj.core :as fs]
   [integrant.core :as ig]))

(def coll-name "kotoris")
(defn coll-path [user-id] (str coll-name "/" user-id))

(defn user-id->doc [db user-id]
  (-> db
      (fs/doc (coll-path user-id))
      (.get)
      (deref)
      (.getData)
      (as-> x (into {} x))
      (keywordize-keys)
      (as-> x (cske/transform-keys csk/->kebab-case-keyword x))))

(defn user-id->creds
  ([db user-id]
   (-> (user-id->doc db user-id)
       (:twitter-auth)
       (as-> x (into {} x))
       (keywordize-keys)
       (rename-keys {:auth_token :auth-token}))))

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

(defn user-id->proxies
  [db user-id]
  (let [doc         (user-id->doc db user-id)
        proxy-label (:proxy-label doc)
        proxy-path  "configs/proxies"]
    (-> db
        (fs/doc proxy-path)
        (.get)
        (deref)
        (.getData)
        (get proxy-label)
        (proxy-fs-http)
        (proxy-port-string->number))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defmethod ig/init-key ::db [_ {:keys [config db]}]
;;   (let [user-id  (:userid config)
;;         doc-path (id->doc-path user-id)
;;         coll     (fs/coll db coll-name)]
;;     (def doc
;;       (-> db
;;           (fs/doc doc-path)
;;           (.get)
;;           (deref)
;;           (.getData)
;;           (as-> x (into {} x))
;;           (keywordize-keys)
;;           (as-> x (cske/transform-keys csk/->kebab-case-keyword x))))
;;     (def twitter-auth (doc->twitter-auth doc))
;;     (let [proxy-label (:proxy-label doc)
;;           proxy-path  "configs/proxies"]
;;       (def proxies
;;         (-> db
;;             (fs/doc proxy-path)
;;             (.get)
;;             (deref)
;;             (.getData)
;;             (get proxy-label)
;;             (proxy-fs-http)
;;             (proxy-port-string->number))))
;;     coll))

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
