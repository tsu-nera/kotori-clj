(ns kotori.domain.dmm.anime
  "ref. https://www.dmm.co.jp/digital/anime/-/genre/"
  (:require
   [kotori.lib.io :as io]))

(def genre-path "dmm/genre/anime.edn")

(defonce genres (->> genre-path io/load-edn))
(defonce genre-name-id-map
  (into {} (map (juxt :name :genre_id) genres)))
(defonce genre-id-name-map
  (into {} (map (juxt :genre_id :name) genres)))

(defn genre-id->name [id] (get genre-id-name-map id))
(defn genre-name->id [name] (get genre-name-id-map name))

(defn genre-names->ids [xs]
  (into #{} (map genre-name->id xs)))

(def dirty-genres
  (genre-names->ids
   ["スカトロ"
    "放尿・お漏らし"
    "浣腸"]))

(def trans-genres
  (genre-names->ids
   ["ゲイ・ホモ"
    "ボーイズラブ"]))

(def violent-genres
  (genre-names->ids
   ["鬼畜"]))

(def antisocial-genres
  (genre-names->ids
   ["監禁"
    "盗撮・のぞき"]))

(def ng-genres
  (into #{} (concat
             antisocial-genres
             violent-genres
             dirty-genres
             trans-genres)))
