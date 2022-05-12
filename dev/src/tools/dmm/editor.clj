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

(def screen-name (->screen-name "0001"))
(def products
  (into []
        (select-scheduled-products {:db          (db-prod)
                                    :limit       20
                                    :screen-name screen-name})))

(def product (nth products 17))
(def title (:title product))
(def names (lib/->actress-names product))
(def ret
  (->> (first names)
       ed/split-actress-name
       flatten
       reverse))
(def titles-no-actress (ed/title->without-actress title names))

(def titles (map :title products))
(def titles-no-actresses (map lib/->title-without-actress products))

#_(defn products->actress-names [products]
(let [actresses (map :actresses products)]
  (map (fn [xm]
         (map (fn [m] (get m "name")) xm)) actresses)))
#_(def names (into [] (map videoc-title->name titles)))
