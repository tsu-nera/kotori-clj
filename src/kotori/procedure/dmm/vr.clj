(ns kotori.procedure.dmm.vr
  (:require
   [kotori.domain.dmm.core :as dmm]
   [kotori.domain.dmm.genre.videoa
    :refer [vr-only-id]
    :rename
    {vr-only-id genre-id}]
   [kotori.domain.dmm.product
    :refer [vr-coll-path]
    :rename
    {vr-coll-path coll-path}]
   [kotori.lib.provider.dmm.product :as lib]
   [kotori.procedure.dmm.product :as product]
   [kotori.procedure.strategy.dmm :as st]))

(def floor (:videoa dmm/floor))

(defn get-products [{:as m}]
  (let [opts {:floor    floor
              :genre-id genre-id}]
    (lib/get-products (merge m opts))))

(defn crawl-product! [{:as m}]
  (product/crawl-product! m coll-path))

(defn crawl-products! [{:as m}]
  (let [opts {:coll-path coll-path
              :genre-id  genre-id
              :floor     floor}]
    (product/crawl-products! (merge m opts))))

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

(comment
  (require '[devtools :refer [code->kotori]]
           '[tools.dmm :refer [creds]]
           '[firebase :refer [db-prod db-dev db]])

  (def products (get-products {:creds (creds)
                               :limit 10}))

  (def resp (crawl-product! {:db    (db-prod)
                             :creds (creds)
                             :cid   "vec00360"}))

  (def resp (crawl-products! {:db    (db-dev)
                              :creds (creds)
                              :limit 10}))

  (def vrs
    (into []
          (st/select-scheduled-products
           {:db    (db-prod)
            :limit 100
            :creds (creds)
            :info  (code->kotori "0028")})))
  )
