(ns kotori.procedure.dmm.vr
  (:require
   [kotori.domain.dmm.product
    :refer [vr-coll-path vr-doc-path]
    :rename {vr-coll-path coll-path vr-doc-path doc-path}]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.product :as lib]))

#_(defn get-products [{:keys [cid env]}]
    (let [{:keys [api-id affiliate-id]}
          env
          creds (api/->Credentials api-id affiliate-id)
          resp  (lib/get-vr-products creds)]
      resp))
