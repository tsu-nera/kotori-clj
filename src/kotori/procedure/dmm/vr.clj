(ns kotori.procedure.dmm.vr
  (:require
   [kotori.domain.dmm.product
    :refer [vr-coll-path vr-doc-path]
    :rename {vr-coll-path coll-path vr-doc-path doc-path}]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.product :as product]))

(defn get-vr-products [{:keys [creds]}]
  (product/get-by-genres creds))

