(ns kotori.lib.provider.dmm.editor
  (:require
   [clojure.string :as str]
   [kotori.domain.dmm.genre.videoc :as videoc]
   [kotori.lib.provider.dmm.parser :as p]))

(defn ->remove-x [x s]
  (let [re (re-pattern x)]
    (-> s
        (str/replace re ""))))

(defn ->remove [x]
  (partial ->remove-x x))

(defn ->remove-xs [s xs]
  (->> xs
       (reduce (fn [s x]
                 ((->remove x) s)) s)
       str/trim))

(defn videoc-title->remove-stopwords [s]
  (->remove-xs s videoc/title-stopwords))

(defn videoc-desc->remove-stopwords [s]
  (->remove-xs s videoc/desc-stopwords))

;; どうもdmmは全角シャープを利用しているように思う.
(defn words->hashtags
  [l]
  (map #(str "＃" %) l))

(defn remove-hashtags
  ([s]
   (let [words (p/->hashtags s)
         tags  (words->hashtags words)]
     (-> s
         (remove-hashtags tags)
         ;; HTML parseでスペースが消える問題の回避のために
         ;; 意図的にparseのときに挿入したスペースをここで除去
         (str/replace #" " ""))))
  ([s hashtags]
   (->remove-xs s hashtags)))

(defn split-actress-name [name]
  (let [re (re-pattern "（(.+?)）$")]
    (if-let [m (re-find re name)]
      [(str/replace name (first m) "") (second m)]
      [name])))

(defn drop-old-name [name]
  (first (str/split name #"（")))

(defn split-actress-names [names]
  (->> names
       (map split-actress-name)
       flatten))

(defn title->without-actress [names title]
  ;; どうもapiで取得できる女優の順番とtitleの順番は一致するようだ.
  ;; リストをひっくり返して後ろから除去
  (let [actress-names (reverse (split-actress-names names))]
    (reduce (fn [title name]
              ;; 末尾のみ除去
              (let [re (re-pattern (str name "$"))]
                (-> title
                    (str/replace re "")
                    str/trim)))
            title actress-names)))

(defn ->sparkle-amateur [s name]
  (let [sparkle       "♀️"
        sparkled-name (str sparkle name)]
    (str/replace s (re-pattern name) sparkled-name)))

(defn ->sparkle-actress [s name]
  (let [sparkle         "✨"
        pattern-1       (str "‘" name "’")
        pattern-2       (str "「" name "」")
        pattern-default name
        alter           (str sparkle name sparkle)]
    (cond
      (str/includes? s pattern-1)
      (str/replace s (re-pattern pattern-1) alter)
      (str/includes? s pattern-2)
      (str/replace s (re-pattern pattern-2) alter)
      (str/includes? s pattern-default)
      (str/replace s (re-pattern pattern-default) alter)
      :else s)))
