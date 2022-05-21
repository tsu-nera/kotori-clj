(ns kotori.lib.provider.dmm.doujin
  (:require
   [kotori.domain.dmm.core :as dmm]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.core :refer [request-bulk]]))

;; APIで取得した情報を転機

(def fanza-doujin
  {:name  "同人",
   :code  "doujin",
   :floor [{:id "81", :name "同人", :code "digital_doujin"}]})

(def service-code (:code fanza-doujin))
(def floor-code "digital_doujin")
(def floor-id 81)

(def base-req-opts
  {:service service-code
   :floor   floor-code})

(defn get-doujin [{:keys [cid creds]}]
  (when-let [resp (api/search-product
                   creds (-> base-req-opts
                             (assoc :cid cid)))]
    (first resp)))

(comment
  (require '[tools.dmm :refer [creds dump-doujin!]])

  ;; CG
  ;; https://www.dmm.co.jp/dc/doujin/-/detail/=/cid=d_205949/
  (def cid "d_205949")
  (def resp (get-doujin {:cid cid :creds (creds)}))
  (dump-doujin! cid)
  )
