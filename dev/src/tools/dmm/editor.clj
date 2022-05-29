(ns tools.dmm.editor
  (:require
   [clojure.string :as str]
   [devtools :refer [->screen-name env]]
   [firebase :refer [db-prod db-dev db]]
   [kotori.lib.kotori :as lib]
   [kotori.lib.provider.dmm.editor :as ed]
   [kotori.lib.provider.dmm.parser :as p]
   [kotori.lib.provider.dmm.public :as public]
   [kotori.procedure.dmm.amateur
    :refer [select-scheduled-products]
    :rename {select-scheduled-products select-scheduled-videocs}]
   [kotori.procedure.strategy.dmm
    :refer [select-scheduled-products]]))

(defn get-desc-from-site [cid floor]
  (:description (public/get-page {:cid cid :floor floor})))

(defn get-desc-raw [cid floor]
  (public/->raw-description
   (public/get-page-raw cid floor)))

(comment
  (def screen-name (->screen-name "0001"))

  (def products
    (into []
          (select-scheduled-products {:db          (db-prod)
                                      :limit       30
                                      :screen-name screen-name})))

  (def product (nth products 12))
  (def title (:title product))
  (def desc (:description product))
  (def names (lib/->actress-names product))
  (def titles-no-actress (ed/title->without-actress names title))

  #_(ed/->sparkle-actress desc (first names))

  (def next-title (lib/title-raw->next title names))
  (def next-desc (lib/desc-raw->next desc names))
  (def next (lib/->next product))

  (def titles (map :title products))
  (def titles-no-actresses (map lib/->title-without-actress products))

  (def names-list (map lib/->actress-names products))

  (def next-descs (map lib/desc-raw->next products))
  #_(def nexts (map lib/->next products))
  )

(comment
  (def screen-name (->screen-name "0027"))

  (def cid "yaho034")
  (def desc (get-desc-from-site cid "videoc"))
  (def raw (get-desc-raw cid "videoc"))
  (public/html->plain-text raw)

  (def tags (p/->hashtags desc))
  (def ret (ed/remove-hashtags desc))
  (def next-desc (lib/desc-raw->next desc []))
  )
