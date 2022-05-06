(ns kotori.domain.dmm.core)

(def doc-path "providers/dmm")

(def field
  {:products-crawled-time "products_crawled_time"
   :vrs-crawled-time      "vrs_crawled_time"
   :animes-crawled-time   "animes_crawled_time"})

(defn ->url [cid]
  (str "https://www.dmm.co.jp/digital/videoa/-/detail/=/cid=" cid "/"))

(defn genres->name-id-map [genres]
  (into {} (map (juxt :name :genre_id) genres)))

(defn genres->id-name-map [genres]
  (into {} (map (juxt :genre_id :name)) genres))

(defn names->genre-ids [name-id-map names]
  (into #{} (map (fn [name]
                   (get name-id-map name)) names)))
