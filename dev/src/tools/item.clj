(ns tools.item)

(defn ->source [items]
  (map (fn [text] {:text text}) items))

(def qvt-0001
  ["やべーよ!"
   "超やべーよ!"
   "まじやべーよ!"
   "くそやべーよ!"
   "クッソやべーよ！"
   "ぱねーよ!"
   "まじぱねーよ!"
   "女神かよ！"
   "天使かよ！"
   "奇跡かよ！"])

(def qvt-dev
  ["ぶぁーか" "かーば" "ブタのけつ"])
