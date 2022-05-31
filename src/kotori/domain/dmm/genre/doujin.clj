(ns kotori.domain.dmm.genre.doujin
  (:require
   [kotori.domain.dmm.core :refer [doc-path]]
   [kotori.domain.dmm.genre.interface :as if]
   [kotori.lib.io :as io]))

(def genre-path "dmm/genre/digital_doujin.edn")

(defonce genres (->> genre-path io/load-edn))
(defonce name-id-map (if/->name-id-map genres))
(defonce id-name-map (if/->id-name-map genres))

(def genre-names->ids (partial if/names->genre-ids name-id-map))

(def coll-path (str doc-path "/doujins"))
(defn ->doc-path [cid] (str coll-path "/" cid))

(defrecord Doujin [floor]
           if/Genre
           (id->name [_ id] (get id-name-map id))
           (name->id [_ name] (get name-id-map name))
           (->coll-path [_] coll-path)
           (->doc-path [_ cid] (->doc-path cid)))

(def voice-ids
  (-> ["焦らし" "言葉責め" "ASMR" "耳舐め" "ささやき" "バイノーラル"]
      genre-names->ids))

(def chikubi-ids
  (-> ["乳首・乳輪" "メスイキ"]
      genre-names->ids))
