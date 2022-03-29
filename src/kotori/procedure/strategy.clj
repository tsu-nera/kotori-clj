(ns kotori.procedure.strategy
  "商品選択戦略"
  (:require
   [clojure.string :as str]
   [kotori.domain.dmm.product :as product]
   [kotori.lib.firestore :as fs]
   [kotori.lib.provider.dmm :as client]))

;; TODO 共通化
(def products-path "providers/dmm/products")

(defn select-product [{:keys [db]}]
  (-> (fs/get-docs db products-path 1)
      vals
      first))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(require '[devtools :refer [env db]])

(def product (select-product {:db (db)}))
