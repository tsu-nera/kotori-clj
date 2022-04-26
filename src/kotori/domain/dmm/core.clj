(ns kotori.domain.dmm.core)

(def doc-path "providers/dmm")

(defn ->url [cid]
  (str "https://www.dmm.co.jp/digital/videoa/-/detail/=/cid=" cid "/"))
