(ns kotori.domain.dmm.genre.interface)

(defprotocol Genre
  (id->name [this id])
  (name->id [this name]))

(defn ->name-id-map [genres]
  (into {} (map (juxt :name :genre_id) genres)))

(defn ->id-name-map [genres]
  (into {} (map (juxt :genre_id :name)) genres))

(defn names->genre-ids [name-id-map names]
  (into #{} (map (fn [name]
                   (get name-id-map name)) names)))
