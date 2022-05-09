(ns kotori.lib.provider.dmm.parser
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [kotori.lib.twittertext :as tt])
  (:import
   (java.text
    BreakIterator)))

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

(defn ->sentences [text]
  (let [locale java.util.Locale/JAPAN
        bit    (doto (BreakIterator/getSentenceInstance locale)
                 (.setText text))]
    (loop [start (.first bit)
           end   (.next bit)
           xs    []]
      (if (= end BreakIterator/DONE)
        xs
        (recur end (.next bit)
               (conj xs (subs text start end)))))))

(defn trunc [n s]
  (str/trim (subs s 0 (min (count s) n))))

(defn join-sentences [length sentences]
  (-> (if (= 1 (count sentences))
        (trunc (first sentences) length)
        (reduce (fn [desc sentence]
                  (if (< length (+ (tt/count desc) (tt/count sentence)))
                    (if (zero? (tt/count desc))
                      sentence
                      desc)
                    (str desc "\n\n" sentence)))
                "" sentences))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;;;;;;;;;;;;;;;;;;;;;;;;;
