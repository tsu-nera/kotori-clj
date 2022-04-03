(ns kotori.domain.tweet.post)

(defn ->coll-path [user-id]  (str "tweets/" user-id "/posts"))

;; (defonce posts nil)
;; (defmethod ig/init-key ::db [_ {:keys [config db]}]
;;   (let [user-id   (:user-id config)
;;         coll-path (id->coll-path user-id)]
;;     (def posts
;;       (-> db
;;           (f/coll coll-path))))
;;   :initalized)
