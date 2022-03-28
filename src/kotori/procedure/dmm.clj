(ns kotori.procedure.dmm
  (:require
   [clojure.string :as str]
   [kotori.domain.dmm.product :as product]
   [kotori.lib.firestore :as fs]
   [kotori.lib.provider.dmm :as client]))

(def products-path "providers/dmm/products")
(def campaigns-path "providers/dmm/campaigns")

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
  [{:keys [env offset hits keyword article article-id] :or {offset 1 hits 100}}]
  (let [{:keys [api-id affiliate-id]} env
        creds                         (client/->Credentials api-id affiliate-id)
        req                           (cond->
                                       {:offset offset :sort "rank" :hits hits}
                                        keyword    (assoc :keyword keyword)
                                        article    (assoc :article article)
                                        article-id (assoc :article_id article-id))
        resp                          (client/search-product creds req)]
    (-> resp
        (->items))))

(defn get-campaign-products "
  キャンペーンの動画一覧の取得は
  keywordにキャンペーン名を指定することで取得可能.
  "
  [{:keys [env title hits]}]
  (get-products {:env env :keyword title :hits hits}))

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
        path    (fs/doc-path products-path cid)]
    (fs/set! db path data)
    data))

(defn crawl-products "
  TODO 500以上の書き込み対応.
  firestroreのbatch writeの仕様で一回の書き込みは500まで.
  そのため500単位でchunkごとに書き込む.
  また Fieldに対するincや配列への追加も1つの書き込みとなる.
  "
  [{:keys [db] :as params}]
  (let [products   (get-products params)
        count      (count products)
        batch-docs (->> products
                        (map product/->data)
                        (fs/make-batch-docs
                         "cid" products-path))]
    (fs/batch-set! db batch-docs)
    {:result "ok"
     :count  count}))

(defn campaign->id
  "引数はDMM APIで取得できた :campaignのkeyに紐づくMapをそのまま利用する.
  {:date_begin \"2022-03-28 10:00:00\",
   :date_end \"2022-03-30 10:09:59\",
   :title \"新生活応援30％OFF第6弾\"}"
  [{:keys [date_begin title]}]
  (let [begin (first (str/split date_begin #" "))]
    (str title "_" begin)))

(defn crawl-campaign-products "
  キャンペーン動画情報をFirestoreに保存する.
  保存の際のキャンペーンIDはキャンペーン期間とタイトルから独自に生成する.
  おそらくキャンペーンタイトルのみをIDにすると定期開催されるものに対応できない.
  "
  [{:keys [db] :as params}]
  (let [products               (get-campaign-products params)
        count                  (count products)
        id                     "新生活応援30％OFF第6弾_2022-03-28"
        campaign-products-path (str campaigns-path "/" id "/products")
        batch-docs             (->> products
                                    (map product/->data)
                                    (fs/make-batch-docs
                                     "cid" campaign-products-path))]
    (fs/batch-set! db batch-docs)
    {:result "ok"
     :count  count}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[devtools :refer [env db]])

  (def product (get-product {:cid "ssis00337" :env (env)}))
  (def products crawl-products {:db (db) :env (env) :hits 40})

  (def products (crawl-campaign-products
                 {:db    (db) :env (env)
                  :title "新生活応援30％OFF第6弾"}))
  )

(comment
  (campaign->id {:date_begin "2022-03-28 10:00:00",
                 :date_end   "2022-03-30 10:09:59",
                 :title      "新生活応援30％OFF第6弾"})
  )
;; => nil;; => nil
