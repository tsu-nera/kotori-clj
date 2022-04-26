(ns kotori.lib.discord
  (:require
   [clj-http.client :as client]
   [kotori.domain.discord :as d]))

(defn- make-req-params [message]
  {:form-params {:content message}})

(defn notify! [channel-name message]
  (let [url (d/get-url channel-name)
        req (make-req-params message)]
    (client/post url req)))

(comment
  (notify! :kotori-qvt "test")
  )
