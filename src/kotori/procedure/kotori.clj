(ns kotori.procedure.kotori
  (:require
   [kotori.domain.kotori :as kotori]
   [kotori.domain.meigen :refer [meigens]]
   [kotori.domain.tweet :refer [posts]]
   [kotori.lib.time :as time]
   [kotori.lib.twitter.guest :as guest]
   [kotori.lib.twitter.private :as private]
   [taoensso.timbre :as log])
  (:import
   (java.text
    SimpleDateFormat)))

(defn pick-random []
  (rand-nth meigens))

(defn make-text [data]
  (let [{content :content, author :author} data]
    (str content "\n\n" author)))

;; (defn parse-timestamp [format timestamp]
;;   (let [locale java.util.Locale/US
;;         sdf    (SimpleDateFormat. format locale)]
;;     (.setTimeZone sdf (java.util.TimeZone/getTimeZone "Asia/Tokyo"))
;;     (.parse sdf timestamp)))

;; (defn parse-twitter-timestamp [timestamp]
;;   (parse-timestamp "EEE MMM dd HH:mm:ss Z yyyy" timestamp))

(defn make-fs-tweet [tweet]
  (let [created_at (time/parse-twitter-timestamp (:created_at tweet))
        user       (:user tweet)]
    {"status_id"  (:id_str tweet)
     "user_id"    (:id_str user)
     "text"       (:text tweet)
     "created_at" created_at
     "updated_at" created_at}))

(defn tweet [{:keys [text screen-name db]}]
  (let [user-id  (guest/resolve-user-id screen-name)
        creds    (kotori/->creds db user-id)
        proxies  (kotori/->proxies db user-id)
        result   (private/create-tweet creds proxies text)
        data     (make-fs-tweet result)
        tweet-id (:id_str result)]
    (try
      (log/info (str "post tweet completed. id=" tweet-id))
      (-> posts
          (.document tweet-id)
          (.set data))
      result
      (catch Exception e (log/error "post tweet Failed." (.getMessage e))))))

(defn tweet-random [{:as params}]
  (let [data (pick-random)
        text (make-text data)]
    (tweet (assoc params :text text))))

(defn tweet-morning
  [{:as params}]
  (tweet (assoc params :text "おはようございます")))

(defn tweet-evening
  [{:as params}]
  (tweet (assoc params :text "お疲れ様です")))

;;;;;;;;;;;;;;;;;;;;
;; Design Journals
;;;;;;;;;;;;;;;;;;;;
(defn dummy [{:keys [text screen-name db] :as params}]
  (let [user-id (guest/resolve-user-id screen-name)
        creds   (kotori/->creds db user-id)
        proxies (kotori/->proxies db user-id)]
    {:text        text
     :screen-name screen-name
     :user-id     user-id
     :creds       creds
     :proxies     proxies}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (def timestamp "2022-02-18 10:00:57")
  (def format "yyyy-MM-dd HH:mm:ss")

  (type (time/parse-timestamp format timestamp))
  )
(comment
  (require '[devtools :refer [db]])

  (def text (make-text (pick-random)))

  (tweet-random {:db          (db)
                 :screen-name "xxxxxxxx"})
  )

(comment
  (require '[local :refer [twitter-auth]])
  (def tweet (private/get-tweet (twitter-auth) "1500694005259980800"))
  (def user (private/get-user twitter-auth proxies "46130870"))
  (def resp (private/create-tweet twitter-auth proxies "test"))

  (def status-id (:id_str resp))
  (def resp (private/delete-tweet twitter-auth proxies status-id))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tweet Data を firestoreへ保存 status_idをidにする

;; (def status (private/update-status "hoge"))
;; (def status_id (:id_str status))

;; (def doc-ref (-> db
;;                  (.collection "tweets")
;;                  (.document status_id)))
;; (def result (. docRef set data))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; retrive meigen from firestore

;; どうもrandom pickをfirestoreでやらせようとしたとき
;; IDを自動生成していると難しいな
;; はじめにmeigensのサイズを取得してクライアント側で乱数生成してrandom pickする
;;
;; 面倒だから全部名言をローカルに取得するかwww

;; (def query (-> fs-coll-meigens
;;                (.get)))
;; ;; (type query)

;; (def query-result @query)
;; (def docs (.getDocuments query-result))

;; (def doc (first docs))
;; (.getData doc)
;; (def data (map #(.getData %) docs))

;; firestore read data.

;; (def doc (-> db
;;              (.collection "users")
;;              (.document "alovlance2")
;;              (.get)))

;; (def data (.getData @doc))

;; // asynchronously retrieve all users
;; ApiFuture<QuerySnapshot> query = db.collection("users").get();
;; // ...
;; // query.get() blocks on response
;; QuerySnapshot querySnapshot = query.get();
;; List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
;; for (QueryDocumentSnapshot document : documents) {
;;                                                   System.out.println ("User: " + document.getId())             ;
;;                                                   System.out.println ("First: " + document.getString("first")) ;
;;                                                   if                 (document.contains("middle"))             {
;;                                                   System.out.println ("Middle: " + document.getString("middle")) ;
;;                                                                                                                 }
;;                                                   System.out.println ("Last: " + document.getString("last"))   ;
;;                                                   System.out.println ("Born: " + document.getLong("born"))     ;
;;                                                   }

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; firebase write data.
;;
;; https://github.com/googleapis/java-firestore/blob/main/samples/snippets/src/main/java/com/example/firestore/Quickstart.java
;;
;; DocumentReference docRef = db.collection("users").document("alovelace");
;; // Add document data  with id "alovelace" using a hashmap
;; Map<String, Object> data = new HashMap<>();
;; data.put("first", "Ada");
;; data.put("last", "Lovelace");
;; data.put("born", 1815);
;; //asynchronously write data
;; ApiFuture<WriteResult> result = docRef.set(data);
;; // ...
;; // result.get() blocks on response
;; System.out.println("Update time : " + result.get().getUpdateTime());

;; (def docRef (-> db
;;                 (.collection "users")
;;                 (.document "alovlance2")))
;; (def data {"first" "Ada"
;;            "last"  "Lovelance"
;;            "born"  1815})
;; (def result (. docRef set data))
