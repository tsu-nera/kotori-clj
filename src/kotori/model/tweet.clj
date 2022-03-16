(ns kotori.model.tweet
  (:require
   [firestore-clj.core :as fs]
   [integrant.core :as ig]))

(defonce posts nil)
(defn id->coll-path [id]  (str "tweets/" id "/posts"))

(defmethod ig/init-key ::db [_ {:keys [config db]}]
  (let [user-id   (:userid config)
        coll-path (id->coll-path user-id)]
    (def posts
      (-> db
          (fs/coll coll-path))))
  :initalized)
