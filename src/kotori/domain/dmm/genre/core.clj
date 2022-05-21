(ns kotori.domain.dmm.genre.core
  (:require
   [kotori.domain.dmm.genre.anime]
   [kotori.domain.dmm.genre.interface :as if]
   [kotori.domain.dmm.genre.videoa]
   [kotori.domain.dmm.genre.videoc])
  (:import
   (kotori.domain.dmm.genre.anime
    Anime)
   (kotori.domain.dmm.genre.videoa
    Videoa)
   (kotori.domain.dmm.genre.videoc
    Videoc)))

(defn make-genre [floor]
  (case floor
    "videoa" (Videoa. "videoa")
    "videoc" (Videoc. "videoc")
    "anime"  (Anime. "anime")
    (Videoa. "videoa")))

(defn id->name [genre id] (if/id->name genre id))
(defn name->id [genre name] (if/name->id genre name))
(defn ->coll-path [genre] (if/->coll-path genre))
(defn ->doc-path [genre cid] (if/->doc-path genre cid))

(comment
  ;; プロトコルに入門しようとしたけどわからなくなっちゃったので中断
  ;; コードは残しておくのでいずれ再挑戦.

  (def videoa (make-genre "videoa"))
  (:floor videoa)
  (if/name->id videoa "ぽっちゃり")
  )
