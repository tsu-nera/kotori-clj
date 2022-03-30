(ns kotori.procedure.strategy
  "商品選択戦略"
  (:require
   [clojure.string :as str]
   [firestore-clj.core :as f]
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
  (let [query-one (fs/query-limit 1)]
    (-> (fs/get-docs db products-path query-one)
        first
        ->next)))

(defn select-scheduled-products [{:keys [db limit] :or {limit 20}}]
  (let [query (fs/query-limit limit)
        docs  (fs/get-docs db products-path query)]
    (map ->next docs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;;;;;;;;;;;
  (require '[devtools :refer [env db]])

  (def product (select-next-product {:db (db)}))
  (->next product)

  (def products
    (into []
          (select-scheduled-products {:db (db) :limit 8})))
 ;;;;;;;;;;;
  )

(comment
  (require '[devtools :refer [env db]])

  (def query (fs/query-limit 5))

  (fs/get-docs (db) "providers/dmm/products" query)
  )
