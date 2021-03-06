(ns kotori.lib.provider.dmm.api
  (:require
   [clj-http.client :as client]
   [kotori.domain.dmm.core :as d]
   [kotori.lib.config :refer [user-agent]]
   [kotori.lib.json :as json]))

(def base-url "https://api.dmm.com/affiliate/v3")
(defn ->endpoint
  [target]
  (str base-url "/" target))

(def base-req-params
  {:site    "FANZA"
   :service "digital"
   :floor   (:videoa d/floor)
   :output  "json"})

(def base-headers {:headers {:user-agent user-agent}})

(defrecord Credentials [^String api-id ^String affiliate-id])

(defn env->creds [env]
  (map->Credentials
   (select-keys env [:affiliate-id :api-id])))

(defn env->creds-swap [env m]
  (let [creds (env->creds env)]
    (-> m
        (assoc :creds creds)
        (dissoc :env))))

(defn- ->result [resp]
  (-> resp
      :result))

(defn- -get [url creds q & {:keys [debug] :or {debug false}}]
  (let [creds-json (json/->json creds)
        params     (merge creds-json base-headers base-req-params q)
        resp       (client/get url
                               {:debug        debug
                                :accept       :json
                                :as           :json
                                :query-params params})]
    (-> resp
        :body
        ->result
        (or (throw (ex-info "Exception occured at dmm http get"
                            {:response resp}))))))

(defn search-product
  "商品検索API: https://affiliate.dmm.com/api/v3/itemlist.html"
  [^Credentials creds q]
  (let [url (->endpoint "ItemList")]
    (-> (-get url creds q) :items)))

(defn get-floors
  "フロアAPI: https://affiliate.dmm.com/api/v3/floorlist.html"
  [^Credentials creds]
  (let [url (->endpoint "FloorList")
        q   {:output "json"}]
    (-get url creds q)))

(defn search-actress
  "女優検索API: https://affiliate.dmm.com/api/v3/actresssearch.html"
  [^Credentials creds q]
  (let [url (->endpoint "ActressSearch")]
    (-get url creds q)))

(defn search-genre
  "ジャンル 検索API: https://affiliate.dmm.com/api/v3/genresearch.html"
  [^Credentials creds q]
  (let [url (->endpoint "GenreSearch")]
    (-get url creds q)))

(defn search-maker
  "メーカー検索API: https://affiliate.dmm.com/api/v3/makersearch.html"
  [^Credentials creds q]
  (let [url (->endpoint "MakerSearch")]
    nil))

(defn search-series
  "シリーズ検索API: https://affiliate.dmm.com/api/v3/seriessearch.html"
  [^Credentials creds q]
  (let [url (->endpoint "SeriesSearch")]
    nil))

;; どうもsite=DMMしか指定できない/site=FANZAは無視されfloor-idも無効.
(defn search-author
  "作者検索API: https://affiliate.dmm.com/api/v3/authorsearch.html"
  [^Credentials creds q]
  (let [url (->endpoint "AuthorSearch")]
    (-get url creds q)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[tools.dmm :refer [creds]])

  (search-product (creds) {:hits    10
                           :sort    "date"
                           :keyword "上原亜衣"})

  (search-product (creds) {:cid "ssis00312"})

  (search-actress (creds) {:actress_id "1008785"})

  (def resp (search-product (creds) {:hits 100 :sort "rank"}))
  )
