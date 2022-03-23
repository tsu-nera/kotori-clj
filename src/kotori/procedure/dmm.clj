(ns kotori.procedure.dmm
  (:require
   [kotori.lib.dmm :as client]))

(defn get-product [{:keys [cid env]}]
  (let [creds {:api_id       (:api-id env)
               :affiliate_id (:affiliate-id env)}]
    (client/search-product creds {:cid cid})))

(comment
  (require '[local :refer [env]])

  (get-product {:cid "ssis00337"
                :env (env)})
  )
