(ns kotori.domain.dmm.core)

(def doc-path "providers/dmm")

(def products-crawled-time "products_crawled_time")
(def vrs-crawled-time "vrs_crawled_time")

(defn ->url [cid]
  (str "https://www.dmm.co.jp/digital/videoa/-/detail/=/cid=" cid "/"))

