(ns devtools
  "REPLからの利用を想定したツール."
  (:require
   [integrant.repl.state :refer [config system]]
   [kotori.domain.kotori.core :refer [config->kotori]]
   [kotori.service.firebase :refer [get-db]]
   [twitter-clj.private :as private]))

(defn db []
  (get-db))

(defn env []
  (get system :kotori.service.env/env))

(defn dev? []
  (= (:env (env)) :development))

(defn prod? []
  (= (:env (env)) :production))

(defn twitter-auth []
  (-> system
      (get :kotori.service.env/env)
      :twitter-auth))

(defn proxies []
  (-> system
      :kotori.service.env/proxies))
#_(proxies)

(defn ->proxy [label]
  (get (proxies) label))

(defn strategies []
  (-> system
      :kotori.service.kotori/strategies))
#_(strategies)

(defn ->strategy [code]
  (get (strategies) code))
#_(->strategy "0001")

(defn kotori-ids []
  (-> system
      :kotori.service.kotori/ids))

(defn kotori-names []
  (-> system
      :kotori.service.kotori/names))
#_(kotori-names)

(defn kotori-codes []
  (-> system
      :kotori.service.kotori/codes))

(defn kotories []
  (-> system
      :kotori.service.kotori/apps))
#_(kotories)

(defn ->kotori [code]
  (get (kotories) code))
#_(->kotori "0001")

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

(defn code->kotori-by-name [screen-name]
  (config->kotori (kotori-by-name screen-name)))

(defn code->kotori [code]
  (config->kotori (kotori-by-code code)))

(defn kotori-params [db code]
  (let [info (config->kotori (kotori-by-code code))]
    {:db db :info info}))

(def info-dev (delay (code->kotori "0003")))

(defn ->screen-name [code]
  (:screen-name (code->kotori code)))

(defn ->user-id [code]
  (:user-id (code->kotori code)))

(defn get-tweet-with-info
  ([code id]
   (let [creds (:creds (code->kotori code))]
     (private/get-tweet creds (str id)))))
#_(get-tweet-with-info "0001" "")

