(ns kotori.firebase
  (:require [clojure.java.io :as io]
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
  (let [project-id  (env :project-id)
        credentials (GoogleCredentials/getApplicationDefault)]
    (-> (FirebaseOptions/builder)
        (.setCredentials credentials)
        (.setProjectId project-id)
        (.build)
        (FirebaseApp/initializeApp))))


(defn get-fs []
  (FirestoreClient/getFirestore))

(def db (delay (f/default-client project-id)))

;; (def db (delay (f/client-with-creds cred-path)))


;; (init-firebase-app-local!)

;; (def db (FirestoreClient/getFirestore))

;;;;;;;;;;;;;;;;;;;;;;
;;  Design Journals
;;;;;;;;;;;;;;;;;;;;;;
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
