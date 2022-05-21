(ns kotori.lib.provider.dmm.ebook
  (:require
   [kotori.domain.dmm.core :as dmm]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.core :refer [request-bulk]]))

;; APIで取得した情報を転機
(def fanza-book
  {:name "FANZAブックス",
   :code "ebook",
   :floor
   [{:id "82", :name "コミック", :code "comic"}
    {:id "83", :name "美少女ノベル・官能小説", :code "novel"}
    {:id "84", :name "アダルト写真集・雑誌", :code "photo"}]})
(def fanza-unlimited-book
  {:name "FANZAブックス読み放題",
   :code "unlimited_book",
   :floor
   [{:id "92", :name "FANZAブックス読み放題", :code "unlimited_comic"}]})

(def service-code (:code fanza-book))
(def floor {:id "82", :name "コミック", :code "comic"})
(def floo-code (:code floor))
(def floor-id (:id floor))

(def base-req-opts
  {:service "ebook"
   :floor   "comic"})

(defn get-comic [{:keys [cid creds]}]
  (when-let [resp (api/search-product
                   creds (-> base-req-opts
                             (assoc :cid cid)))]
    (first resp)))

(defn get-unlimited-comic
  "なんか取得できない..."
  [{:keys [cid creds]}]
  (when-let [resp (api/search-product
                   creds {:service "unlimited_book"
                          :floor   "unlimited_comic"
                          :cid     cid})]
    (first resp)))

(comment
  (require '[tools.dmm :refer [creds]])

  (def cid "b915awnmg01007")

  (def resp (get-comic {:cid cid :creds (creds)}))
  (def resp (get-unlimited-comic {:cid   "b425aakkg00201"
                                  :creds (creds)}))
  )
