(ns kotori.domain.dmm.product
  (:require
   [clojure.walk :refer [stringify-keys]]))

(defn ->doc-data [product]
  ;; (let [[:content_id cid] product]
  {:cid (:content_id product)
   :raw product}
  ;; )
  )
