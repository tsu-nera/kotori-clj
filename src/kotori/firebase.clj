(ns kotori.firebase
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]
            [environ.core :refer [env]]
            [firestore-clj.core :as f])
  (:import (com.google.auth.oauth2 GoogleCredentials)
           (com.google.firebase FirebaseApp FirebaseOptions)
           (com.google.firebase.cloud FirestoreClient)
           ))

(def cred-path (env :credentials-path))
(def project-id (env :project-id))

(defn init-firebase-app-local! []
  (let [service-account (io/input-stream cred-path)
        credentials     (GoogleCredentials/fromStream service-account)]
    (-> (FirebaseOptions/builder)
        (.setCredentials credentials)
        (.build)
        (FirebaseApp/initializeApp))))

(defn init-firebase-app-prod! []
  (let [credentials (GoogleCredentials/getApplicationDefault)]
    (-> (FirebaseOptions/builder)
        (.setCredentials credentials)
        (.setProjectId project-id)
        (.build)
        (FirebaseApp/initializeApp))))


(defn get-fs []
  (FirestoreClient/getFirestore))

(def db (delay (f/default-client project-id)))

;; (defrecord FirebaseBoundary [firebase])

(defmethod ig/init-key ::firebase [_ _]
  (println "init firebase instance.")
  (init-firebase-app-local!))

(defmethod ig/halt-key! ::firebase [_ _]
  (println "destroy firebase instantce."))

;;;;;;;;;;;;;;;;;;;;;;
;;  Design Journals
;;;;;;;;;;;;;;;;;;;;;;

;; TODO 初期化失敗時の処理を検討. i.e. firestore接続不可.

;; ::firebaseはreader macroで :kotori.firebase/firebaseと同義.

;; (defmethod ig/init-key ::firebase
;;   [_ {:keys [env]}]
;;   (let [firebase-credentials (:firebase-credentials env)
;;         firebase-options     (FirebaseOptions/builder)
;;         firebaseApp          (-> firebase-options
;;                                  (.setCredentials firebase-credentials)
;;                                  .build
;;                                  FirebaseApp/initializeApp)]
;;     (timbre/info "connectiong to firebase with " firebase-credentials)
;;     (->FirebaseBoundary {:firebase-app  firebaseApp
;;                          :firebase-auth (FirebaseAuth/getInstance)})))

;; (defmethod ig/halt-key! ::firebase
;;   [_ boundary]
;;   (->
;;    boundary
;;    .firebase
;;    :firebase-app
;;    .delete))

;; (def db (delay (f/client-with-creds cred-path)))

;; (init-firebase-app-local!)

;; (def db (FirestoreClient/getFirestore))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; https://firebase.google.com/docs/admin/setup/
;; FirebaseOptions options = FirebaseOptions.builder()
;;     .setCredentials(GoogleCredentials.getApplicationDefault())
;;     .setDatabaseUrl("https://<DATABASE_NAME>.firebaseio.com/")
;;     .build();
;; FirebaseApp.initializeApp(options);

;; https://firebase.google.com/docs/firestore/quickstart?hl=ja
;; https://github.com/googleapis/java-firestore/tree/main/samples/snippets/src/main/java/com/example/firestore
;;
;; import com.google.auth.oauth2.GoogleCredentials;
;; import com.google.cloud.firestore.Firestore;

;; import com.google.firebase.FirebaseApp;
;; import com.google.firebase.FirebaseOptions;

;; // Use a service account
;; InputStream serviceAccount = new FileInputStream("path/to/serviceAccount.json");
;; GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
;; FirebaseOptions options = new FirebaseOptions.Builder()
;;     .setCredentials(credentials)
;;     .build();
;; FirebaseApp.initializeApp(options);

;; Firestore db = FirestoreClient.getFirestore();

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; import com.google.auth.oauth2.GoogleCredentials;
;; import com.google.cloud.firestore.Firestore;

;; import com.google.firebase.FirebaseApp;
;; import com.google.firebase.FirebaseOptions;

;; // Use the application default credentials
;; GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
;; FirebaseOptions options = new FirebaseOptions.Builder()
;; .setCredentials(credentials)
;; .setProjectId(projectId)
;; .build();
;; FirebaseApp.initializeApp(options);

;; Firestore db = FirestoreClient.getFirestore();
