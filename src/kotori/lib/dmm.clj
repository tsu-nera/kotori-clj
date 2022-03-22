(ns kotori.lib.dmm
  (:require
   [clj-http.client :as client]
   [kotori.lib.config :refer [user-agent]]))

(def api-url "https://api.dmm.com/affiliate")
(def version "v3")
(def base-url (str api-url "/" version))

(def itemlist-url (str base-url "/" "ItemList"))

(def base-req-params
  {:site    "FANZA"
   :service "digital"
   :floor   "videoa"
   :output  "json"})

(def headers {:headers {:user-agent user-agent}})

(def creds {})

(def req-params
  (merge creds headers base-req-params
         {:hits    10
          :sort    "date"
          :keyword "上原亜衣"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(def resp
    (client/get itemlist-url
                {:debug        true
                 :as           :json
                 :query-params req-params}))
