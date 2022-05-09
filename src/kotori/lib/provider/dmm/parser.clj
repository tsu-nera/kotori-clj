(ns kotori.lib.provider.dmm.parser
  (:require
   [clojure.string :as str]))

(defn ->remove-x [x s]
  (let [re (re-pattern x)]
    (-> s
        (str/replace re ""))))

(defn ->hashtags [s]
  (let [re (re-pattern "[#|＃](.+?)[ |【]")]
    (->> s
         (re-seq re)
         (map second)
         (map str/trim)
         (map #(str "＃" %)))))

(defn ->remove-hashtags
  ([s]
   (let [tags (->hashtags s)]
     (->remove-hashtags s tags)))
  ([s hashtags]
   (let [ret (reduce (fn [s tag]
                       ((partial ->remove-x tag) s)) s hashtags)]
     (-> ret
         (str/trim)))))
