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
  #{4021 5015})

(def violent-genre-ids
  #{21 567 5059 6094 6953})

(def dirty-genre-ids
  #{4018 5007 5011 5012 5013 5014 5024 6151})

(def ng-genres
  (into #{} (concat
             vr-genre-ids
             antisocial-genre-ids
             violent-genre-ids
             dirty-genre-ids)))

(defn ->next [product]
  "表示用に情報を間引くことが目的."
  (let [raw     (-> product
                    (dissoc :legacy)
                    (dissoc :raw))
        cid     (:cid raw)
        title   (:title raw)
        genres  (str/join "," (map #(get % "name") (:genres raw)))
        ranking (:rank-popular raw)]
    (select-keys raw [:cid :title])
    {:cid     cid
     :title   title
     :genres  genres
     :ranking ranking
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

(def strategy-popular
  (fs/query-order-by "last_crawled_time" :desc
                     "rank_popular" :asc))

;; TODO 遅延シーケンスとaccumulatorで必要な分のは取得するように改善したい.
;; (.listDocuments coll) で DocumentReferenceのlistを取得可能.
;; referenceいうことはまだ通信は発生していない.
;; このリストから必要な分だけ評価してaccumulateする.
;; とりあえず今はちょっと多めに取得した上で最後に必要な分だけ限定する.
(defn select-scheduled-products [{:keys [db limit] :or {limit 5}}]
  (let [limit-plus (int (* 1.5 limit))
        q-limit    (fs/query-limit limit-plus)
        queries    (fs/make-xquery [q-limit strategy-popular])
        products   (fs/get-docs db products-path queries)]
    (->> products
         exclude-ng-genres
         (take limit))))

(defn select-next-product [{:keys [db]}]
  (first (select-scheduled-products {:db db})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;;;;;;;;;;;
  (require '[devtools :refer [env db]])

  (def product (select-next-product {:db (db)}))
  (->next product)

  (str/join "," (map #(get % "name") genres))

  (def products
    (into []
          (select-scheduled-products {:db (db) :limit 5})))

  (map ->next products)
 ;;;;;;;;;;;
  )

(comment
  (require '[devtools :refer [env db]])

  (def query (fs/query-limit 5))

  (fs/get-docs (db) "providers/dmm/products" query)
  )
