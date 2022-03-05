(ns kotori.kotori
  (:require
   [integrant.core :as ig]
   [firestore-clj.core :as f]
   [clojure.walk :refer [keywordize-keys]]))

(def doc (atom nil))
(def coll-name "kotoris")

(defn id->coll-path [id]  (str coll-name "/" id))

(defmethod ig/init-key ::app [_ {:keys [config db]}]
  (let [user-id   (:user-id config)
        coll-path (id->coll-path user-id)]
    (reset! doc (-> db
                    (f/doc coll-path)
                    (.get)
                    (deref)
                    (.getData)
                    (as-> x (into {} x))
                    (keywordize-keys))))
  :initalized)

(defmethod ig/halt-key! ::app [_ _]
  :terminated)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  doc
  @doc
  )
