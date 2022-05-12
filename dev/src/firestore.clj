(ns firestore
  (:require
   [clojure.java.browse :as b]
   [kotori.domain.dmm.product :as dmm]))

"https://console.cloud.google.com/firestore/data/providers/dmm/products/fcdc00141?project=dmm-fanza"

(def fs-base-url "https://console.cloud.google.com/firestore/data/")

(defn ->product-path [cid]
  (str fs-base-url dmm/coll-path "/"  cid "?project=dmm-fanza"))

(defn open-dmm-fs-url [cid]
  (b/browse-url (->product-path cid)))
