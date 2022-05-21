(ns kotori.domain.dmm.genre.anime
  "ref. https://www.dmm.co.jp/digital/anime/-/genre/"
  (:require
   [kotori.domain.dmm.core :refer [doc-path]]
   [kotori.domain.dmm.genre.interface :as if]
   [kotori.lib.io :as io]))

(def genre-path "dmm/genre/anime.edn")

(defonce genres (->> genre-path io/load-edn))
(defonce name-id-map (if/->name-id-map genres))
(defonce id-name-map (if/->id-name-map genres))

(def genre-names->ids (partial if/names->genre-ids name-id-map))

(def coll-path (str doc-path "/animes"))
(defn ->doc-path [cid] (str coll-path "/" cid))

(defrecord Anime [floor]
           if/Genre
           (id->name [_ id] (get id-name-map id))
           (name->id [_ name] (get name-id-map name))
           (->coll-path [_] coll-path)
           (->doc-path [_ cid] (->doc-path cid)))

(def dirty-ids
  (genre-names->ids ["スカトロ" "放尿・お漏らし" "浣腸"]))

(def trans-ids
  (genre-names->ids ["ゲイ・ホモ" "ボーイズラブ"]))

(def violent-ids
  (genre-names->ids ["鬼畜"]))

(def antisocial-ids
  (genre-names->ids ["監禁" "盗撮・のぞき"]))

(def ng-genres
  (into #{} (concat
             antisocial-ids
             violent-ids
             dirty-ids
             trans-ids)))
