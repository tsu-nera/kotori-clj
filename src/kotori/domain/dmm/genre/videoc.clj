(ns kotori.domain.dmm.genre.videoc
  "ref. https://www.dmm.co.jp/digital/videoc/-/genre/"
  (:require
   [kotori.domain.dmm.genre.core :as genre]
   [kotori.domain.dmm.genre.interface :as if]
   [kotori.lib.io :as io]))

(def genre-path "dmm/genre/videoc.edn")

(defonce genres (->> genre-path io/load-edn))
(defonce name-id-map (genre/->name-id-map genres))
(defonce id-name-map (genre/->id-name-map genres))

(def genre-names->ids (partial genre/names->genre-ids name-id-map))

(defmethod if/id->name :videoc [_ id]
  (get id-name-map id))

(def dirty-ids
  (genre-names->ids ["スカトロ" "放尿・お漏らし" "脱糞"
                     "浣腸" "異物挿入"]))

(def violent-ids
  (genre-names->ids ["鬼畜"]))

(def antisocial-ids
  (genre-names->ids ["監禁" "盗撮・のぞき" "ドラッグ"]))

(def ng-genres
  (into #{} (concat
             antisocial-ids
             violent-ids
             dirty-ids)))
