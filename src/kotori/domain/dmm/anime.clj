(ns kotori.domain.dmm.anime
  "ref. https://www.dmm.co.jp/digital/anime/-/genre/"
  (:require
   [kotori.domain.dmm.core :as core]
   [kotori.lib.io :as io]))

(def genre-path "dmm/genre/anime.edn")

(defonce genres (->> genre-path io/load-edn))
(defonce genre-name-id-map (core/genres->name-id-map genres))
(defonce genre-id-name-map (core/genres->id-name-map genres))

(def genre-names->ids (partial core/names->genre-ids genre-name-id-map))

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
