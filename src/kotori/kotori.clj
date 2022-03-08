(ns kotori.kotori
  (:require
   [clojure.set :refer [rename-keys]]
   [integrant.core :as ig]
   [firestore-clj.core :as fs]
   [camel-snake-kebab.extras :as cske]
   [camel-snake-kebab.core :as csk]
   [clojure.walk :refer [keywordize-keys]]
   [kotori.twitter.private :as private]))

(defn id->coll-path [id]  (str "kotoris/" id))

(defn doc->twitter-auth
  [doc]
  (-> doc
      (:twitter-auth)
      (as-> x (into {} x))
      (keywordize-keys)
      (rename-keys {:auth_token :auth-token})))


(def doc (atom nil))
(def twitter-auth (atom {}))
(def proxy (atom {}))

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

(defmethod ig/init-key ::app [_ {:keys [config db]}]
  (let [user-id   (:user-id config)
        coll-path (id->coll-path user-id)]
    (reset! doc (-> db
                    (fs/doc coll-path)
                    (.get)
                    (deref)
                    (.getData)
                    (as-> x (into {} x))
                    (keywordize-keys)
                    (as-> x (cske/transform-keys csk/->kebab-case-keyword x))))
    (reset! twitter-auth (doc->twitter-auth @doc))
    (let [proxy-label (:proxy-label @doc)
          proxy-path  "configs/proxies"]
      (reset! proxy (-> db
                        (fs/doc proxy-path)
                        (.get)
                        (deref)
                        (.getData)
                        (get proxy-label)
                        (proxy-fs-http)
                        (proxy-port-string->number)))))
  :initalized)

(defmethod ig/halt-key! ::app [_ _]
  :terminated)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment

  (-> db-tmp
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

  (def tweet (private/get-tweet @twitter-auth @proxy "1500694005259980800"))
  (def user (private/get-user @twitter-auth @proxy "46130870"))
  (def resp (private/create-tweet @twitter-auth @proxy "test"))

  (def status-id (:id_str resp))
  (def resp (private/delete-tweet @twitter-auth @proxy status-id))
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
