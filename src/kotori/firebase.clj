(ns kotori.firebase
  (:require [clojure.java.io :as io]
            [integrant.core :as ig])
  (:import (com.google.auth.oauth2 GoogleCredentials)
           (com.google.firebase FirebaseApp FirebaseOptions)
           (com.google.firebase.cloud FirestoreClient)))

;; (def cred-path (env :credentials-path))
;; (def project-id (env :project-id))

(defn init-firebase-app-cred!
  [cred-path]
  (let [service-account (io/input-stream cred-path)
        credentials     (GoogleCredentials/fromStream service-account)]
    (-> (FirebaseOptions/builder)
        (.setCredentials credentials)
        (.build)
        (FirebaseApp/initializeApp))))

(defn init-firebase-app-default!
  [project-id]
  (let [credentials (GoogleCredentials/getApplicationDefault)]
    (-> (FirebaseOptions/builder)
        (.setCredentials credentials)
        (.setProjectId project-id)
        (.build)
        (FirebaseApp/initializeApp))))

(defn init-firebase-app!
  [config]
  (if (:local? config)
    (init-firebase-app-cred! (:cred-path config))
    (init-firebase-app-default! (:project-id config))))

;; FirebaseAppとFirestoreに関わるシングルトンな状態管理は
;; integrantにおまかせするので自分で状態は持たない.
(defmethod ig/init-key ::firebase [_ {:keys [config]}]
  (let [app (init-firebase-app! config)
        db  (FirestoreClient/getFirestore app)]
    (println "create FirebaseApp instance.")
    {:app app :db db}))

;; Firesore InterfaceはAutoClosableというInteraceを実装しているようで
;; 名前からしてFirebaseAppを消せば勝手にFirestoreも消えそうだな.
(defmethod ig/halt-key! ::firebase [_ {:keys [app]}]
  (println "destroy FirebaseApp instantce.")
  (.delete app))

;;;;;;;;;;;;;;;;;;;;;;
;;  Design Journals
;;;;;;;;;;;;;;;;;;;;;;

;; TODO 初期化失敗時の処理を検討. i.e. firestore接続不可.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
