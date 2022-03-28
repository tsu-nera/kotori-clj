(ns devtools
  (:require
   [integrant.repl.state :refer [config system]]
   [kotori.procedure.dmm :refer [get-product]]
   [kotori.service.firebase :refer [get-app get-db delete-app!]]))

(defn db []
  (get-db))

(defn env []
  (get system :kotori.service.env/env))

(defn dev? []
  (= (:env (env)) :development))

(defn prod? []
  (= (:env (env)) :production))

(defn get-dmm-product [cid]
  (get-product {:cid cid :env (env)}))
#_(get-dmm-product "ssis00337")
