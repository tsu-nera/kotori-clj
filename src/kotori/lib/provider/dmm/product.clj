(ns kotori.lib.provider.dmm.product
  (:require
   [kotori.domain.dmm.core :as dmm]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.core :refer [request-bulk]]))

(defn get-video [{:keys [cid creds floor]
                  :or   {floor (:videoa dmm/floor)}}]
  (when-let [resp (api/search-product
                   creds {:cid cid :floor floor})]
    (first resp)))

(defn get-videoa [{:as m}]
  (get-video (assoc m :floor (:videoa dmm/floor))))

(defn get-videoc [{:as m}]
  (get-video (assoc m :floor (:videoc dmm/floor))))

(defn get-anime [{:as m}]
  (get-video (assoc m :floor (:anime dmm/floor))))

(defn get-products-by-cids
  "APIの並列実行をする.呼び出し回数制限もあるためリストのサイズに注意"
  [{:keys [creds cids floor]}]
  (let [products (->> cids
                      (map (fn [cid] {:creds creds :cid cid}))
                      (pmap (fn [m] (get-video m floor)))
                      (doall))]
    (into [] products)))

(defn- get-products-chunk "
  この関数では100個のまでの商品取得に対応. hits ~< 100まで.
  100件以上の取得はget-productsで対応."
  [{:keys [creds hits] :or {hits 100} :as params}]
  {:pre [(<= hits 100)]}
  (let [req   (assoc params :sort "rank")
        items (api/search-product creds req)]
    items))

(defn- make-offset-map [size offset]
  {:offset offset
   :hits   size})

(defn- make-req-params [limit floor]
  (let [size         100
        page         (quot limit size)
        ->offset-map (partial make-offset-map size)
        xf           (comp          (map #(+ (* % size) 1))
                                    (map ->offset-map)
                                    (map #(assoc % :floor floor)))
        mod-hits     (mod limit size)]
    (cond-> (into [] xf (range page))
      (not (= 0 mod-hits))
      (conj (-> (make-offset-map mod-hits (+ 1 (* page size)))
                (assoc :floor floor))))))

(defn get-products "
  get-productsを呼ぶと1回のget requestで最大100つの情報が取得できる.
  それ以上取得する場合はoffsetによる制御が必要なためこの関数で対応する.
  limitを100のchunkに分割してパラレル呼び出しとマージ."
  [{:keys [creds limit floor]
    :as   base-params
    :or   {limit 5 floor (:videoa dmm/floor)}}]
  (let [req-params (map (fn [m] (merge base-params m))
                        (make-req-params limit floor))]

    (->> req-params
         (request-bulk get-products-chunk)
         (reduce concat)
         (into []))))

(defn- ->genre-req [genre-id]
  {:article (:genre dmm/article) :article_id genre-id})

(defn get-by-genre [{:keys [genre-id creds]}]
  (let [q (->genre-req genre-id)]
    (->> (api/search-product creds q)
         (into []))))

(defn get-by-genres
  "複数genre-idをパラレルで取得して結果をマージ."
  [{:keys [genre-ids creds]}]
  (->> genre-ids
       (map #(->genre-req %))
       (pmap #(api/search-product creds %))
       flatten
       (into #{})
       (into [])))

(comment
  (require '[tools.dmm :refer [creds]])

  (def products (get-products {:creds @creds :limit 10}))
  (def products (get-products {:creds @creds :limit 110}))
  )
