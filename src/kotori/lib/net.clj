(ns kotori.lib.net
  (:require
   [clj-http.client :as client]))

(defn get-global-ip []
  (:body (client/get "http://inet-ip.info/ip")))

#_(get-global-ip)
