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
  (genre-names->ids ["監禁" "盗撮・のぞき" "ドラッグ"]))

(def ng-genres
  (into #{} (concat
             antisocial-ids
             violent-ids
             dirty-ids)))
