(ns kotori.domain.dmm.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]))

(def doc-path "providers/dmm")
(def vr-coll-path (str doc-path "/vrs"))
(def amateur-coll-path (str doc-path "/amateurs"))

(s/def ::cid string?)

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

(defn ->timestamp-key
  ([floor]
   (->timestamp-key floor "default"))
  ([floor genre-id]
   (str/join "." ["last_crawled_time" floor "genres" (str genre-id)])))

;; TODO 削除
(def field
  {:products-crawled-time "products_crawled_time"
   :vrs-crawled-time      "vrs_crawled_time"
   :animes-crawled-time   "animes_crawled_time"
   :amateurs-crawled-time "amateurs_crawled_time"})

(defn ->url
  ([cid]
   (->url "videoa" cid))
  ([floor cid]
   (->url "digital" floor cid))
  ([service floor cid]
   (str "https://www.dmm.co.jp/"
        service "/" floor "/-/detail/=/cid=" cid "/")))
