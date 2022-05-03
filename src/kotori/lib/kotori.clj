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

(defn- trunc
  [s n]
  (subs s 0 (min (count s) n)))

(defn join-sentences [sentences & {:keys [length] :or {length 80}}]
  (-> (if (= 1 (count sentences))
        (trunc (first sentences) length)
        (reduce (fn [desc sentence]
                  (if (< length (+ (count desc) (count sentence)))
                    desc
                    (str desc "\n\n" sentence)))
                "" sentences))
      str/trim))

(defn desc->trimed
  [text]
  (-> text
      trim-headline
      desc->sentences
      join-sentences))

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
  (def sample "女神の美体から汗、涎、愛液、潮…全エキスが大・放・出！体液まみれでより一層エロさを増した美乃すずめが快楽のまま本能全開で汁だくSEX！絶頂に次ぐ絶頂、意識朦朧となるほどの本気の交わりで大量失禁＆大量イキ潮スプラッシュ！全身ぐっちょり、体液滴るイイ女が性欲尽きるまでイッてイッてイキまくる！！")
  (def ret (desc->trimed sample))

  )
