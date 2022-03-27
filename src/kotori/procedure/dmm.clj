(ns kotori.procedure.dmm
  (:require
   [firestore-clj.core :as f]
   [kotori.domain.dmm.product :as product]
   [kotori.lib.firestore :as fs]
   [kotori.lib.json :as json]
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
    (-> db
        (f/doc path)
        (f/set! data))))

(defn crawl-products "
  firestroreのbatch writeの仕様で一回の書き込みは500まで.
  そのため500単位でchunkごとに書き込む.
  また Fieldに対するincや配列への追加も1つの書き込みとなる.
  "
  [{:keys [db] :as m}]
  ())

(comment
  (require '[local :refer [env db]])
  (def product (get-product {:cid "ssis00337" :env (env)}))

  (product/->data product)
  (tap> product)

  (:content_id product)

  (product/->obj product)

  (defn tmp [{:keys [cid db] :as m}]
    (let [path (str "providers/dmm/products/" cid)]
      path))
  (f/doc (db) "providers/dmm/products/ssis00337")

  (crawl-product {:cid "ssis00337" :env (env) :db (db)})

  (def products (get-products {:env (env)}))
  (def products2 (get-products {:env (env) :offset 101}))
  (def products3 (get-products {:env (env) :offset 201}))
  (count products)

  (count (get-products-bulk {:page 10 :env (env)}))
  )

(comment
  (require '[local :refer [env db]])
  (def docs (get-products {:env (env) :hits 20}))


  (def one (first docs))
  (product/->data one)

  (def b (atom (f/batch (db))))
  (->> docs
       (map product/->data)
       (map (fn [data]
              (let [id (get data "cid")]
                {:id   id
                 :path (str "providers/dmm/products/" id)
                 :data data})
              ))
       (map (fn [m] (fs/set (db) @b #p (:path m) (:data m)))))
  (f/commit! @b)

  (defn set-doc [doc]
    (let id (get "id" doc))
    )

  (->> docs
       (json/->json)
       (map ))
  )
