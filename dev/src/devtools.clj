(ns devtools
  "REPLからの利用を想定したツール."
  (:require
   [integrant.repl.state :refer [config system]]
   [kotori.domain.kotori :as d]
   [kotori.lib.twitter.guest :as guest]
   [kotori.lib.twitter.private :as private]
   [kotori.procedure.dmm :refer [get-product get-products]]
   [kotori.procedure.kotori :refer [make-info]]
   [kotori.service.firebase :refer [get-app get-db delete-app!]]))

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

(defn dmm-creds []
  (-> system
      (get :kotori.service.env/env)
      (select-keys [:api-id :affiliate-id])))

(defn kotori-configs [])

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

(defn ->screen-name [code]
  (:screen-name (kotori-info code)))

(defn ->user-id [code]
  (:user-id (kotori-info code)))

(defn get-tweet-private
  ([id]
   (private/get-tweet (twitter-auth) (str id)))
  ([screen-name id]
   nil))

(defn get-dmm-product [cid]
  (get-product {:env (env) :cid cid}))
#_(get-dmm-product "ssis00337")

(defn get-dmm-campaign [title]
  (get-products {:env (env) :hits 10 :keyword title}))
#_(get-dmm-campaign "新生活応援30％OFF第6弾")
