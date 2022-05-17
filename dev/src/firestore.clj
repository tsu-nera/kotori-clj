(ns firestore
  (:require
   [clojure.java.browse :as b]
   [kotori.domain.dmm.product :as dmm]))

"https://console.cloud.google.com/firestore/data/providers/dmm/products/fcdc00141?project=dmm-fanza"

(def fs-base-url "https://console.cloud.google.com/firestore/data/")

(defn ->product-path [doc-path]
  (str fs-base-url doc-path "?project=dmm-fanza"))

(defn open-doc [doc-path cid]
  (b/browse-url (->product-path (doc-path cid))))

(defn open-product
  [cid]
  (open-doc dmm/doc-path cid))

(defn open-amateur
  [cid]
  (open-doc dmm/amateur-doc-path cid))

(defn open-anime
  [cid]
  (open-doc dmm/anime-doc-path cid))

(defn open-vr
  [cid]
  (open-doc dmm/vr-doc-path cid))
