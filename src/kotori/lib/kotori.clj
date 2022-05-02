(ns kotori.lib.kotori
  (:require
   [clojure.string :as str]
   [kotori.domain.config.ngword :refer [source]]))

(defn desc->headline [text]
  (let [re (re-pattern "^＜(.+?)＞|^【(.+?)】")]
    (first (re-find re text))))

(defn desc->dialogue [text]
  (let [re (re-pattern "「(.+?)」")]
    (when-let [dialogue (first (re-find re text))]
      (if (< 6 (count dialogue))
        dialogue))))

(defn trim-headline [text]
  (if-let [headline (desc->headline text)]
    (-> text
        (str/replace headline ""))
    text))

(defn desc->sentences [text]
  (-> text
      (str/replace #"。。。" "。")
      (str/replace #"。" "。\n")
      (str/replace #"！！" "！！\n")
      (str/replace #"！？" "！？\n")
      (str/replace #"…！" "…！\n")
      str/split-lines))

(defn join-sentences [sentences & {:keys [length] :or {length 100}}]
  (str/trim (reduce (fn [desc sentence]
                      (if (< length (count desc))
                        desc
                        (str desc "\n\n" sentence)))
                    "" sentences)))

(defn desc->trimed
  [text]
  (-> text
      trim-headline
      desc->sentences
      join-sentences))

(defn- trunc
  [s n]
  (subs s 0 (min (count s) n)))

(defn ng->ok [text]
  (when text
    (reduce (fn [x [k v]]
              (str/replace x k v)) text source)))

(defn ->next
  [product]
  (let [cid     (:cid product)
        title   (-> (:title product) ng->ok)
        desc    (-> (:description product)
                    ng->ok
                    desc->trimed)
        summary (-> (:summary product) ng->ok)]
    {:cid         cid
     :title       title
     :description desc
     :summary     summary
     ;; :headline    (desc->headline desc)
     ;; :dialogue    (desc->dialogue desc)
     }))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[firebase :refer [db-prod db-dev db]]
           '[devtools :refer [->screen-name env]]
           '[kotori.procedure.strategy.dmm
             :refer [select-scheduled-products]])

  (def screen-name (->screen-name "0001"))
  (def products
    (into []
          (select-scheduled-products {:db          (db-prod)
                                      :limit       20
                                      :screen-name screen-name})))
  (def descs (map :description products))

  (map desc->dialogue descs)
  (map desc->headline descs)
  (println (nth (map desc->trimed descs) 2))

  (def trimed (map desc->trimed descs))
  )
