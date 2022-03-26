(ns kotori.lib.provider.dmm
  (:require
   [clj-http.client :as client]
   [kotori.lib.config :refer [user-agent]]
   [kotori.lib.json :as json]))

(def base-url "https://api.dmm.com/affiliate/v3")

(defn ->endpoint
  [target]
  (str base-url "/" target))

(def base-req-params
  {:site    "FANZA"
   :service "digital"
   :floor   "videoa"
   :output  "json"})

(def base-headers {:headers {:user-agent user-agent}})

(defrecord Credentials [^String api_id ^String affiliate_id])

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
        (or (throw (ex-info "Exception occured at dmm http get"
                            {:response resp}))))))

(defn search-product
  "商品検索API: https://affiliate.dmm.com/api/v3/itemlist.html"
  [^Credentials creds q]
  (let [url (->endpoint "ItemList")]
    (-get url creds q)))

(defn get-floors
  "フロアAPI: https://affiliate.dmm.com/api/v3/floorlist.html"
  [^Credentials creds q]
  (let [url (->endpoint "FloorList")]
    nil))

(defn search-actress
  "女優検索API: https://affiliate.dmm.com/api/v3/actresssearch.html"
  [^Credentials creds q]
  (let [url (->endpoint "ActressSearch")]
    (-get url creds q)))

(defn search-genre
  "ジャンル 検索API: https://affiliate.dmm.com/api/v3/genresearch.html"
  [^Credentials creds q]
  (let [url (->endpoint "GenreSearch")]
    nil))

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

(defn search-author
  "作者検索API: https://affiliate.dmm.com/api/v3/authorsearch.html"
  [^Credentials creds q]
  (let [url (->endpoint "AuthorSearch")]
    nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[local :refer [dmm-creds]])
  (def creds (map->Credentials (dmm-creds)))

  (search-product creds {:hits    10
                         :sort    "date"
                         :keyword "上原亜衣"})
  (search-product creds {:cid "ssis00312"})

  (search-actress creds {:actress_id "1008785"})

  (def resp (search-product creds {:hits 100 :sort "rank"}))
  )
