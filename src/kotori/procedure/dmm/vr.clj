(ns kotori.procedure.dmm.vr
  (:require
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.product
    :refer [vr-coll-path]
    :rename
    {vr-coll-path coll-path}]
   [kotori.domain.dmm.videoa
    :refer [vr-only-id]
    :rename
    {vr-only-id genre-id}]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.product :as lib]
   [kotori.lib.time :as time]
   [kotori.procedure.dmm.product :as product]
   [kotori.procedure.strategy.dmm :as st]))

(defn get-products [{:as params}]
  (let [opts {:floor      (:videoa api/floor)
              :article    (:genre api/article)
              :article_id genre-id}]
    (lib/get-products (merge params opts))))

(defn crawl-product! [{:as m}]
  (product/crawl-product! m coll-path))

(defn crawl-products!
  [{:keys [db] :as m}]
  (let [timestamp-key (:vrs-crawled-time dmm/field)
        ts            (time/fs-now)]
    (when-let [products (get-products m)]
      (doto db
        (product/save-products! coll-path products ts)
        (product/update-crawled-time! timestamp-key ts)
        (product/scrape-desc-if! coll-path timestamp-key))
      {:timestamp ts
       :count     (count products)
       :products  products})))

;; サンプル動画として公開されて利用できるものは半年以上前のものばかり.
;; https://www.dmm.co.jp/litevideo/-/list/=/article=keyword/id=6793/
;;
;; 最新作はいちおうホスティングはされているものの,
;; 2画面に分割されているサンプル動画であるので直接みるには不向き.
;; https://cc3001.dmm.co.jp/vrsample/s/siv/sivr00201/sivr00201vrlite.mp4
;;
;; 手動でも自動でも一応ダウンロードはできるがあえて規約違反を犯すリスクを
;; 取りつつダウンロードして利用するべきでないため動画は選択ロジックから除外.
;; よい素材が今後開放されることを願いつつ待つ.
(defn select-scheduled-products [{:as m :keys [db limit] :or {limit 5}}]
  (let [xst      [st/st-exclude-ng-genres
                  st/st-exclude-movie
                  st/st-exclude-no-image
                  st/st-exclude-omnibus
                  st/st-include-vr]
        params   (st/assoc-last-crawled-time
                  m db (:vrs-crawled-time dmm/field))
        products (st/select-scheduled-products-with-xst
                  params xst coll-path)]
    (->> products
         (sort-by :rank-popular)
         (take limit))))

(comment
  (require '[devtools :refer [env ->screen-name]]
           '[tools.dmm :refer [creds]]
           '[firebase :refer [db-prod db-dev db]])

  (def products (get-products {:creds @creds
                               :limit 10}))

  (def resp (crawl-product! {:db  (db)
                             :env (env)
                             :cid "bibivr00059"}))

  (def resp (crawl-products! {:db    (db-dev)
                              :env   (env)
                              :limit 300}))


  (def vrs
    (into []
          (select-scheduled-products
           {:db          (db-prod)
            :limit       10
            :screen-name (->screen-name "0028")})))
  )
