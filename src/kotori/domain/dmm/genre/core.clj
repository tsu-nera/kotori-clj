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
  (id->name "videoa" nil)
  )
