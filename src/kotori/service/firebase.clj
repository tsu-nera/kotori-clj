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
  [cred-path]
  (let [service-account (io/input-stream cred-path)
        credentials     (GoogleCredentials/fromStream service-account)]
    (-> (FirebaseOptions/builder)
        (.setCredentials credentials)
        (.build)
        (FirebaseApp/initializeApp))))

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
  [config]
  (init-firebase-app-cred! (:cred-path config)))

(defn get-app []
  (FirebaseApp/getInstance))

(defn delete-app! []
  (.delete (get-app)))

(defn get-db []
  (FirestoreClient/getFirestore))

(defmethod ig/init-key ::app [_ {:keys [config]}]
  (let [env (:env config)
        app (create-app! config)]
    (println "create FirebaseApp instance" env)
    {:app app :env env}))

;; Firesore InterfaceはAutoClosableというInteraceを実装しているようで
;; 名前からしてFirebaseAppを消せば勝手にFirestoreも消えそうだな.
(defmethod ig/halt-key! ::app [_ {:keys [app env]}]
  (println "destroy FirebaseApp instance" env)
  (.delete app))

;; FirebaseAppとFirestoreに関わるシングルトンな状態管理は
;; integrantにおまかせするので自分で状態は持たない.
(defmethod ig/init-key ::db [_ {:keys [app]}]
  (println "create Firestore instance" (:env app))
  (FirestoreClient/getFirestore))

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
  )

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
