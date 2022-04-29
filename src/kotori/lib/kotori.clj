(ns kotori.lib.kotori
  (:require
   [clojure.string :as str]
   [kotori.domain.config.ngword :refer [source]]))

(defn desc->headline-arrow [text]
  (when-let [raw (-> text
                     (str/split #"＞")
                     first
                     (str/split #"＜")
                     second)]
    (str "＜" raw "＞")))

(defn desc->headline-paren [text]
  (when-let [raw (-> text
                     (str/split #"】")
                     first
                     (str/split #"【")
                     second)]
    (str "【" raw "】")))

(defn desc->headline
  [text]
  (if-let [arrow (desc->headline-arrow text)]
    arrow
    (when-let [paren (desc->headline-paren text)]
      paren)))

;; (defn desc->dialogue
;;   [text]
;;   (when-let [raw (-> text
;;                      (str/split #"^$「")
;;                      second
;;                      (str/split #"」")
;;                      first)]
;;     (str "「" raw "」")))

;; (defn desc->trimed
;;   [text & {:keys [length] :or {length 60}}]
;;   (-> (if-let [headline (desc->headline text)]
;;         (str/replace text headline "")
;;         text)
;;       (trunc length)
;;       (str "...")))

(defn- trunc
  [s n]
  (subs s 0 (min (count s) n)))

(defn desc->trimed
  [text & {:keys [length] :or {length 60}}]
  (-> text
      (trunc length)
      (str "...")))

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
  ;;;
  (def desc
    "＜8年の時を経て同じ男にレ●プされる不条理トラウマ作品、第2弾！＞学生時代に犯●れ、男の人に恐怖を抱くようになったはなはトラウマを抱えたまま大人に。家族で田舎に逃げ平穏を取り戻したはずだったが、釈放されたレ●プ魔に見つかり再び惨劇が始まる。8年間女を抱けずに溜まった精子と性欲をぶちまけに来た男に押し潰され、重量差に骨は軋み、呼吸器官を圧迫され、苦しみながら何度も中出しされる地獄の3日間。")

  (def headline (desc->headline desc))
  (def trimed (desc->trimed desc))
  )
