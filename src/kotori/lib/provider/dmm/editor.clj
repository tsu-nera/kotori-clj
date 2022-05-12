(ns kotori.lib.provider.dmm.editor
  (:require
   [clojure.string :as str]
   [kotori.lib.provider.dmm.parser :as p]))

(def videoc-title-stopwords
  ["（仮名）" "（裏垢）" "天才"])

(def videoc-desc-stopwords
  ["いつもオナ素材としてのご利用ありがとうございます。"
   "たかまり↑おじさんです。"
   "ハメ撮り同人AV活動で生計を立てております。"
   "※本編顔出しです。"])

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
  (->remove-xs s videoc-title-stopwords))

(defn videoc-desc->remove-stopwords [s]
  (->remove-xs s videoc-desc-stopwords))

;; どうもdmmは全角シャープを利用しているように思う.
(defn words->hashtags
  [l]
  (map #(str "＃" %) l))

(defn remove-hashtags
  ([s]
   (let [words (p/->hashtags s)
         tags  (words->hashtags words)]
     (remove-hashtags s tags)))
  ([s hashtags]
   (->remove-xs s hashtags)))

(defn split-actress-name [name]
  (let [re (re-pattern "（(.+?)）$")]
    (if-let [m (re-find re name)]
      [(str/replace name (first m) "") (second m)]
      [name])))

(defn title->without-actress [title names]
  ;; どうもapiで取得できる女優の順番とtitleの順番は一致するようだ.
  ;; リストをひっくり返して後ろから除去
  (let [actress-names (->> names
                           (map split-actress-name)
                           flatten
                           reverse)]
    (reduce (fn [title name]
              ;; 末尾のみ除去
              (let [re (re-pattern (str name "$"))]
                (-> title
                    (str/replace re "")
                    str/trim)))
            title actress-names)))

#_(defn ->sparkle-actress [s names]
    (map ->title-without-actress products))
