(ns tools.dmm.editor
  (:require
   [clojure.string :as str]
   [devtools :refer [->screen-name env]]
   [firebase :refer [db-prod db-dev db]]
   [kotori.lib.kotori :as lib]
   [kotori.lib.provider.dmm.editor :as ed]
   [kotori.lib.provider.dmm.parser :as p]
   [kotori.procedure.dmm.amateur
    :refer [select-scheduled-products]
    :rename {select-scheduled-products select-scheduled-videocs}]
   [kotori.procedure.strategy.dmm
    :refer [select-scheduled-products]]))

(comment
  (def screen-name (->screen-name "0001"))

  (def products
    (into []
          (select-scheduled-products {:db          (db-dev)
                                      :limit       50
                                      :screen-name screen-name})))

  (def product (nth products 1))
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
#_(defn products->actress-names [products]
(let [actresses (map :actresses products)]
  (map (fn [xm]
         (map (fn [m] (get m "name")) xm)) actresses)))
#_(def names (into [] (map videoc-title->name titles)))
