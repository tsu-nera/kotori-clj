(ns devtools
  "REPLからの利用を想定したツール."
  (:require
   [integrant.repl.state :refer [config system]]
   [kotori.lib.twitter.guest :as guest]
   [kotori.lib.twitter.private :as private]
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

(defn get-tweet-guest [id]
  (guest/get-tweet id))

(defn twitter-auth []
  (-> system
      (get :kotori.service.env/env)
      (select-keys [:auth-token :ct0])))

(defn get-tweet-private
  ([id]
   (private/get-tweet (twitter-auth) (str id)))
  ([screen-name id]
   nil))
