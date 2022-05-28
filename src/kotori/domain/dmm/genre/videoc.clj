(ns kotori.domain.dmm.genre.videoc
  "ref. https://www.dmm.co.jp/digital/videoc/-/genre/"
  (:require
   [kotori.domain.dmm.core :refer [doc-path]]
   [kotori.domain.dmm.genre.interface :as if]
   [kotori.lib.io :as io]))

(def genre-path "dmm/genre/videoc.edn")

(defonce genres (->> genre-path io/load-edn))
(defonce name-id-map (if/->name-id-map genres))
(defonce id-name-map (if/->id-name-map genres))

(def genre-names->ids (partial if/names->genre-ids name-id-map))

(def coll-path (str doc-path "/amateurs"))
(defn ->doc-path [cid] (str coll-path "/" cid))

(defrecord Videoc [floor]
           if/Genre
           (id->name [_ id] (get id-name-map id))
           (name->id [_ name] (get name-id-map name))
           (->coll-path [_] coll-path)
           (->doc-path [_ cid] (->doc-path cid)))

(def dirty-ids
  (genre-names->ids ["スカトロ" "放尿・お漏らし" "脱糞"
                     "浣腸" "異物挿入"]))

(def violent-ids
  (genre-names->ids ["鬼畜"]))

(def antisocial-ids
  (genre-names->ids ["監禁" "盗撮・のぞき" "ドラッグ" "拘束"]))

(def ng-genres
  (into #{} (concat
             antisocial-ids
             violent-ids
             dirty-ids)))

(def title-stopwords
  ["（仮名）" "（裏垢）" "天才"])

(def desc-stopwords
  ["いつもオナ素材としてのご利用ありがとうございます。たかまり↑おじさんです。ハメ撮り同人AV活動で生計を立てております。"
   "いつもおせわになっております。"
   "二代目つば飲みおじさんでございます。"
   "202X年、世界はウイルスの渦に包まれた。ナンパは枯れ、合コンは裂け、全ての出会いが壊滅したかのように見えた。だが、ワンナイは死滅していなかった！世はマッチアプリが支配する弱肉以下略。"
   "性帝サウザーです。"
   "素人ホイホイstayhome"
   "こんにちは、恋愛に人生を捧げているヘイタです。"
   "刺さった街で女性をナンパする企画ダーツナンパ in Tokyo ！"
   "↑サンプル必見↑"
   "※本編顔出しです。"])
