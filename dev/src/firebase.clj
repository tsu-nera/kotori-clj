(ns firebase
  (:require
   [clojure.java.io :as io]
   [integrant.core :as ig]
   [kotori.service.firebase :refer [create-app! get-db]]))

(def creds-dev "private/dev/credentials.json")
(def creds-prod "private/prod/credentials.json")

;; i.e. name=[DEFAULT]
(def db (delay (get-db)))
(def db-dev (delay (get-db "dev")))
(def db-prod (delay (get-db "prod")))

(defmethod ig/init-key ::app [_ _]
  (let [dev  (create-app! (io/resource creds-dev)  "dev")
        prod (create-app! (io/resource creds-prod) "prod")]
    {:dev dev :prod prod}))

(defmethod ig/halt-key! ::app [_ {:keys [dev prod]}]
  (.delete dev)
  (.delete prod))
