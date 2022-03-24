(ns kotori.domain.tweet
  (:require
   [firestore-clj.core :as f]))

(defonce posts nil)

(defn id->coll-path [id]  (str "tweets/" id "/posts"))

;; (defmethod ig/init-key ::db [_ {:keys [config db]}]
;;   (let [user-id   (:user-id config)
;;         coll-path (id->coll-path user-id)]
;;     (def posts
;;       (-> db
;;           (f/coll coll-path))))
;;   :initalized)
