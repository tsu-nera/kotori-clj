(ns kotori.lib.dmm
  (:require
   [clj-http.client :as client]
   [kotori.lib.config :refer [user-agent]]))

(def base-url "https://api.dmm.com/affiliate/v3")

(defn ->endpoint
  [target]
  (str base-url "/" target))

(def base-req-params
  {:site    "FANZA"
   :service "digital"
   :floor   "videoa"
   :output  "json"})

(def headers {:headers {:user-agent user-agent}})

(defrecord Credentials [^String api_id ^String affiliate_id])

(defn- get [url params & {:keys [debug] :or {debug false}}]
  (client/get url
              {:debug        debug
               :as           :json
               :query-params params}))

(defn search-product
  "商品検索API: https://affiliate.dmm.com/api/v3/itemlist.html"
  [^Credentials creds q]
  (let [url    (->endpoint "ItemList")
        params (merge creds headers base-req-params
                      {:hits    10
                       :sort    "date"
                       :keyword "上原亜衣"})]
    (get url params {:debug false})))

(defn get-floors
  "フロアAPI: https://affiliate.dmm.com/api/v3/floorlist.html"
  [^Credentials creds q]
  (let [url (->endpoint "FloorList")]
    nil))

(defn search-actress
  "女優検索API: https://affiliate.dmm.com/api/v3/actresssearch.html"
  [^Credentials creds q]
  (let [url (->endpoint "ActressSearch")]
    nil))

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
  (def creds (->Credentials "xxxxxxx" "xxxxxxxxxxx"))

  (search-product creds {})
  )
