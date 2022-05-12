(ns firestore
  (:require
   [clojure.java.browse :as b]
   [kotori.domain.dmm.product :as dmm]))

"https://console.cloud.google.com/firestore/data/providers/dmm/products/fcdc00141?project=dmm-fanza"

(def fs-base-url "https://console.cloud.google.com/firestore/data/")

(defn ->product-path [doc-path]
  (str fs-base-url doc-path "?project=dmm-fanza"))

(defn open-amateur
  [cid]
  (b/browse-url (->product-path (dmm/amateur-doc-path cid))))

(defn open-product
  [cid]
  (b/browse-url (->product-path (dmm/doc-path cid))))
