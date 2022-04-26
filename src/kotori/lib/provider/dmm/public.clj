(ns kotori.lib.provider.dmm.public
  (:require
   [clj-http.client :as client]
   [kotori.domain.dmm.core :as domain]
   [kotori.lib.config :refer [user-agent]]))

(def headers
  {:user-agent user-agent
   :cookie     "age_check_done=1"})

(defn get-page [{:keys [cid]}]
  (let [url (domain/->url cid)]
    (client/get url {:headers headers})))

(comment
  (def cid "pfes00034")
  (def page (get-page {:cid cid}))
  )
