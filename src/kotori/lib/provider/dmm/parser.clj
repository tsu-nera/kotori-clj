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

(defn remove-hashtags
  ([s]
   (let [tags (->hashtags s)]
     (remove-hashtags s tags)))
  ([s hashtags]
   (let [ret (reduce (fn [s tag]
                       ((partial ->remove-x tag) s)) s hashtags)]
     (str/trim ret))))

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
  (subs s 0 (min (count s) n)))

(defn- join [length xs]
  (let [s-first (first xs)
        s-rest  (rest xs)]
    (reduce (fn [acc s]
              (let [s-cat (str acc "\n\n" s)]
                (if (< length (tt/count s-cat))
                  acc
                  s-cat)))
            s-first s-rest)))

(defn generate-comb [xs]
  (let [size (count xs)]
    (cond->>
     (partition (- size 1) 1 xs)
      (> size 3) (concat (partition (- size 2) 1 xs)))))

(defn join-sentences [length xs]
  (let [s-all (str/join "\n\n" xs)
        size  (count xs)]
    (cond
      (= 1 size)                  (trunc length (first xs))
      (> length (tt/count s-all)) s-all
      :else
      (->> xs
           (take 6)
           generate-comb
           (map (partial join length))
           (apply max-key count)))))

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
