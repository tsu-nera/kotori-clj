(ns devtools
  "REPLからの利用を想定したツール."
  (:require
   [integrant.repl.state :refer [config system]]
   [kotori.procedure.dmm :refer [get-product get-products get-campaign-products]]
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
  (get-product {:env (env) :cid cid}))
#_(get-dmm-product "ssis00337")

(defn get-dmm-campaign [title]
  (get-products {:env (env) :hits 10 :keyword title}))
#_(get-dmm-campaign "新生活応援30％OFF第6弾")
