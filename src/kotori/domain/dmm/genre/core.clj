(ns kotori.domain.dmm.genre.core
  (:require
   [kotori.domain.dmm.genre.interface :as if]))

(defn ->name-id-map [genres]
  (into {} (map (juxt :name :genre_id) genres)))

(defn ->id-name-map [genres]
  (into {} (map (juxt :genre_id :name)) genres))

(defn names->genre-ids [name-id-map names]
  (into #{} (map (fn [name]
                   (get name-id-map name)) names)))

(defn id->name [floor id]
  (if/id->name (keyword floor) id))

(comment
  ;; プロトコルに入門しようとしたけどわからなくなっちゃったので中断
  ;; コードは残しておくのでいずれ再挑戦.
  (defprotocol IGenre
    (id->name [this id])
    (name->id [this name]))

  (defrecord Genre
      [floor id-name-map name-id-map]
    IGenre
    (id->name [_ id] (get id-name-map id))
    (name->id [_ name] (get name-id-map name)))

  (defn make-genre [floor]
    (->Genre floor nil nil))

  (def videoa (make-genre "videoa"))
  (:floor videoa)
  (id->name videoa 2007)
  )
