(ns tools.dmm.product
  (:require
   [firebase :refer [db-prod]]
   [kotori.domain.dmm.core :refer [->product-doc-path]]
   [kotori.lib.firestore :as fs]))

(defn update-ignore!
  [coll-name cid flag]
  (let [doc-path (->product-doc-path coll-name cid)]
    (fs/assoc! (db-prod) doc-path "ignore" flag)))
#_(update-ignore! "products" "bab00058" true)
