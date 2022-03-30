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

(def vr-genre-ids
  #{6793 6925})

(def antisocial-genre-ids
  "Twitter的にダメなジャンル."
  #{5015 4021})

(def violent-genre-ids
  #{567 5059 6094 6953 21})

(def dirty-genre-ids
  #{5007 5012 5013 5014 6151 4018 5011 5024})

(def ng-genres
  (into #{} (concat
             vr-genre-ids
             antisocial-genre-ids
             violent-genre-ids
             dirty-genre-ids)))

(defn ->next [product]
  "表示用に情報を間引くことが目的."
  (let [raw    (-> product
                   (dissoc :legacy)
                   (dissoc :raw))
        cid    (:cid raw)
        title  (:title raw)
        genres (str/join "," (map #(get % "name") (:genres raw)))]
    (select-keys raw [:cid :title])
    {:cid    cid
     :title  title
     :genres genres
     ;; :raw    raw
     }))

(defn ng-genre? [id]
  (contains? ng-genres id))

(defn ng-product? [product]
  (some true? (map
               (comp ng-genre? #(get % "id"))
               (:genres product))))

(defn exclude-ng-genres "
  firestore側で除外のフィルタリングをすることができない(難しい)ため,
  アプリ側で除外を実施する."
  [products]
  (remove ng-product? products))

(defn select-next-product [{:keys [db]}]
  (-> (fs/get-docs db products-path fs/query-one)
      first))

(defn select-scheduled-products [{:keys [db limit] :or {limit 20}}]
  (let [query    (fs/query-limit limit)
        products (fs/get-docs db products-path query)]
    (exclude-ng-genres products)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;;;;;;;;;;;
  (require '[devtools :refer [env db]])

  (def product (select-next-product {:db (db)}))
  (->next product)

  (str/join "," (map #(get % "name") genres))

  (def products
    (into []
          (select-scheduled-products {:db (db) :limit 8})))

  (map ->next products)
 ;;;;;;;;;;;
  )

(comment
  (require '[devtools :refer [env db]])

  (def query (fs/query-limit 5))

  (def q-exclude-genres )

  (fs/get-docs (db) "providers/dmm/products" query)
  )