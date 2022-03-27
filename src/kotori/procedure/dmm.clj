(ns kotori.procedure.dmm
  (:require
   [kotori.domain.dmm.product :as product]
   [kotori.lib.firestore :as fs]
   [kotori.lib.provider.dmm :as client]))

(defn- ->items [resp]
  (-> resp
      :result
      :items))

(defn get-product [{:keys [cid env]}]
  (let [{:keys [api-id affiliate-id]} env
        creds                         (client/->Credentials api-id affiliate-id)
        resp                          (client/search-product creds {:cid cid})]
    (-> resp
        (->items)
        (first))))

(defn get-products "
  1回のget requestで最大100つの情報が取得できる.
  それ以上取得する場合はoffsetによる制御が必要.
  "
  [{:keys [env offset hits] :or {offset 1 hits 100}}]
  (let [{:keys [api-id affiliate-id]} env
        creds                         (client/->Credentials api-id affiliate-id)
        req                           {:offset offset :sort "rank" :hits hits}
        resp                          (client/search-product creds req)]
    (-> resp
        (->items))))

(defn get-products-bulk "
  TODO 並列処理改善. うまくできているか怪しい.
  "
  [{:keys [env page]}]
  (let [products (->> (range page)
                      (map #(+ (* % 100) 1))
                      (map (fn [offset]
                             {:env env :offset offset :hits 100}))
                      (pmap get-products)
                      (doall))
        results  (reduce concat products)]
    results))

(defn crawl-product "
  1. 指定されたcidのcontent情報を取得.
  2. Firestoreへ 情報を保存."
  [{:keys [db cid] :as m}]
  (let [product (get-product m)
        data    (product/->data product)
        path    (str "providers/dmm/products/" cid)]
    (fs/set! db path data)))

(defn crawl-products "
  TODO 500以上の書き込み対応.
  firestroreのbatch writeの仕様で一回の書き込みは500まで.
  そのため500単位でchunkごとに書き込む.
  また Fieldに対するincや配列への追加も1つの書き込みとなる.
  "
  [{:keys [db] :as params}]
  (let [products-path "providers/dmm/products/"
        products      (get-products params)
        batch-docs    (->> products
                           (map product/->data)
                           (fs/make-batch-docs
                            "cid" products-path))]
    (fs/batch-set! db batch-docs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[local :refer [env db]])

  (def product (get-product {:cid "ssis00337" :env (env)}))
  (def products crawl-products {:db (db) :env (env) :hits 40})
  )
