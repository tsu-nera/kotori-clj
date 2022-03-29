(ns kotori.procedure.strategy
  "商品選択戦略"
  (:require
   [clojure.string :as str]
   [kotori.domain.dmm.product :as product]
   [kotori.lib.firestore :as fs]
   [kotori.lib.provider.dmm :as client]))

;; TODO 共通化
(def products-path "providers/dmm/products")

(defn ->next [product]
  (let [raw   (-> product
                  (dissoc :legacy)
                  (dissoc :raw))
        cid   (:cid raw)
        title (:title raw)]
    {:cid   cid
     :title title
     ;;:raw   raw
     }))

(defn select-next-product [{:keys [db]}]
  (-> (fs/get-docs db products-path 1)
      first
      ->next))

(defn select-scheduled-products [{:keys [db limit] :or {limit 20}}]
  (let [docs (fs/get-docs db products-path limit)]
    (map ->next docs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;;;;;;;;;;;
  (require '[devtools :refer [env db]])

  (def product (select-next-product {:db (db)}))

  (def products
    (into []
          (select-scheduled-products {:db (db) :limit 8})))

  (->next product)
  ;;;;;;;;;;;
  )
