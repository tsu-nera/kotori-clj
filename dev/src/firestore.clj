(ns firestore
  (:require
   [clojure.java.browse :as b]
   [kotori.domain.dmm.genre.anime :as anime]
   [kotori.domain.dmm.genre.videoa :as videoa]
   [kotori.domain.dmm.genre.videoc :as videoc]))

(def fs-base-url "https://console.cloud.google.com/firestore/data/")

(defn ->product-path [doc-path]
  (str fs-base-url doc-path "?project=dmm-fanza"))

(defn open-doc [doc-path cid]
  (b/browse-url (->product-path (doc-path cid))))

(defn open-product
  [cid]
  (open-doc videoa/->doc-path cid))

(defn open-amateur
  [cid]
  (open-doc videoc/->doc-path cid))

(defn open-anime
  [cid]
  (open-doc anime/->doc-path cid))

(defn open-vr
  [cid]
  ;; TODO
  (open-doc "providers/dmm/vrs" cid))
