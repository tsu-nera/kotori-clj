(ns kotori.lib.provider.dmm.parser
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [kotori.lib.twittertext :as tt]))

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

(defn- trunc
  [s n]
  (subs s 0 (min (count s) n)))

(defn join-sentences [sentences & {:keys [length] :or {length 80}}]
  (-> (if (= 1 (count sentences))
        (trunc (first sentences) length)
        (reduce (fn [desc sentence]
                  (if (< length (+ (count desc) (count sentence)))
                    (if (zero? (count desc))
                      (trunc sentence length)
                      desc)
                    (str desc "\n\n" sentence)))
                "" sentences))
      str/trim))

(defn check! [s]
  (let [test (tt/parse s)]
    (pp/pprint test)
    test))

(defn test!
  ([result original]
   (println "original:")
   (println original)
   (println)
   (test! result))
  ([result]
   (println "result:")
   (println result)
   (println)
   (println "lenth:")
   (check! result)
   (println "---")))
