(ns kotori.lib.provider.dmm.parser
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [kotori.lib.twittertext :as tt])
  (:import
   (java.text
    BreakIterator)))

(defn ->hashtags [s]
  (let [re (re-pattern "[#|＃](.+?)[ |【]")]
    (->> s
         (re-seq re)
         (map second)
         (map str/trim)
         (map #(str "＃" %)))))

(defn join-until [xs limit]
  (let [first-s (first xs)
        rest-xs (rest xs)]
    (loop [s   (first rest-xs)
           xs  (rest rest-xs)
           acc first-s]
      (let [s-cat (str acc "、"  s)]
        (if (< limit (count s-cat))
          acc
          (recur (first xs) (rest xs) s-cat))))))

(defn split-long-sentence [s]
  (let [limit  60
        length (count s)]
    (if (< length limit)
      s
      (let [sep     "、"
            re      (re-pattern sep)
            xs      (str/split s re)
            first-s (str (join-until xs limit) sep)]
        (if (= 1 (count xs))
          [s]
          [first-s (str/replace s first-s "")])))))

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

(defn join [length xs]
  (let [s-cat (str/join "\n\n" xs)]
    (if (> length (tt/count s-cat))
      s-cat
      (let [s-first (first xs)
            s-rest  (rest xs)]
        (loop [acc s-first
               s   (first s-rest)
               xs  (rest s-rest)]
          (let [s-cat (str acc "\n\n" s)]
            (if (< length (tt/count s-cat))
              acc
              (recur s-cat (first xs) (rest xs)))))))))

(defn generate-comb [xs]
  (let [size (count xs)]
    (cond->>
     (partition (- size 1) 1 xs)
      (> size 3) (concat (partition (- size 2) 1 xs)))))

(defn join-sentences [length xs]
  (let [s-all (str/join "\n\n" xs)
        size  (count xs)]
    (cond
      (= 1 size)                  (first xs)
      (> length (tt/count s-all)) s-all
      :else
      (->> xs
           (take 6)
           generate-comb
           (map (partial join length))
           (apply max-key count)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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
