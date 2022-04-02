(ns kotori.service.firebase
  (:require
   [clojure.java.io :as io]
   [integrant.core :as ig])
  (:import
   (com.google.auth.oauth2
    GoogleCredentials)
   (com.google.firebase
    FirebaseApp
    FirebaseOptions)
   (com.google.firebase.cloud
    FirestoreClient)))

(defn init-firebase-app-cred!
  ([cred-path]
   ;; 名前なしでinitializeAppを呼ぶと[DEFAULT]という名前になるようなので
   ;; 明示的に指定しておく.
   (init-firebase-app-cred! cred-path "[DEFAULT]"))
  ([cred-path name]
   (let [service-account (io/input-stream cred-path)
         credentials     (GoogleCredentials/fromStream service-account)]
     (-> (FirebaseOptions/builder)
         (.setCredentials credentials)
         (.build)
         (FirebaseApp/initializeApp name)))))

;; 一旦使わない方向で検討する.
;; (defn init-firebase-app-default!
;;   [project-id]
;;   (let [credentials (GoogleCredentials/getApplicationDefault)]
;;     (-> (FirebaseOptions/builder)
;;         (.setCredentials credentials)
;;         (.setProjectId project-id)
;;         (.build)
;;         (FirebaseApp/initializeApp))))

(defn create-app!
  ([creds-path]
   (init-firebase-app-cred! creds-path))
  ([creds-path name]
   (init-firebase-app-cred! creds-path name)))

(defn get-app
  ([]
   (FirebaseApp/getInstance))
  ([name]
   (FirebaseApp/getInstance name)))

(defn delete-app!
  ([]
   (.delete (get-app)))
  ([name]
   (.delete (get-app name))))

(defn get-db
  ([]
   (FirestoreClient/getFirestore))
  ([name]
   (FirestoreClient/getFirestore (get-app name))))

(defmethod ig/init-key ::app [_ {:keys [creds-path]}]
  (let [app (create-app! creds-path)]
    (println "create FirebaseApp instance")
    app))
;; Firesore InterfaceはAutoClosableというInteraceを実装しているようで
;; 名前からしてFirebaseAppを消せば勝手にFirestoreも消えそうだな.
(defmethod ig/halt-key! ::app [_ app]
  (println "destroy FirebaseApp instance")
  (.delete app))

;; FirebaseAppとFirestoreに関わるシングルトンな状態管理は
;; integrantにおまかせするので自分で状態は持たない.
(defmethod ig/init-key ::db [_ app]
  (get-db))

;; Firesore InterfaceはAutoClosableというInteraceを実装しているようで
;; 名前からしてFirebaseAppを消せば勝手にFirestoreも消えそうだな.
;; (defmethod ig/halt-key! ::db [_ _]
;;   (println "destroy FirebaseApp instance")
;;   (.delete (FirebaseApp/getInstance)))

;;;;;;;;;;;;;;;;;;;;;;
;;  Design Journals
;;;;;;;;;;;;;;;;;;;;;;
(comment
  (FirebaseApp/getInstance)
  (.delete (FirebaseApp/getInstance))
  )
