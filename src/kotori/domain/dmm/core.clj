(ns kotori.domain.dmm.core)

(def doc-path "providers/dmm")

(def floor
  "ref. https://affiliate.dmm.com/api/v3/floorlist.html"
  {;
   :videoa "videoa" ; ビデオ
   :videoc "videoc" ; 素人
   :anime  "anime"  ; アニメ動画
   })

(def article
  {;
   :genre   "genre"   ; ジャンル
   :actress "actress" ; 女優
   })

(def field
  {:products-crawled-time "products_crawled_time"
   :vrs-crawled-time      "vrs_crawled_time"
   :animes-crawled-time   "animes_crawled_time"
   :amateurs-crawled-time "amateurs_crawled_time"})

(defn ->url
  ([cid]
   (->url cid (:videoa floor)))
  ([cid floor]
   (str "https://www.dmm.co.jp/digital/"
        floor "/-/detail/=/cid=" cid "/")))

(defn genres->name-id-map [genres]
  (into {} (map (juxt :name :genre_id) genres)))

(defn genres->id-name-map [genres]
  (into {} (map (juxt :genre_id :name)) genres))

(defn names->genre-ids [name-id-map names]
  (into #{} (map (fn [name]
                   (get name-id-map name)) names)))
