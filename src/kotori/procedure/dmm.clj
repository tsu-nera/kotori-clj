(ns kotori.procedure.dmm
  (:require
   [kotori.lib.dmm :as client]))

(defn get-product [{:keys [cid env]}]
  (let [{:keys [api-id affiliate-id]} env
        creds
        (client/->Credentials api-id affiliate-id)]
    (client/search-product creds {:cid cid})))

(comment
  (require '[local :refer [env]])

  (get-product {:cid "ssis00337"
                :env (env)})
  )
