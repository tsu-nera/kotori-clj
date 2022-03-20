(ns kotori.procedure.kotori
  (:require
   [kotori.lib.twitter.private :as private]
   [kotori.model.kotori :refer [twitter-auth proxies]]
   [kotori.model.meigen :refer [meigens]]
   [kotori.model.tweet :refer [posts]]
   [taoensso.timbre :as log])
  (:import
   (java.text
    SimpleDateFormat)))

(defn pick-random []
  (rand-nth meigens))

(defn make-status [data]
  (let [{content :content, author :author} data]
    (str content "\n\n" author)))

(defn parse-twitter-timestamp [timestamp]
  (let [format "EEE MMM dd HH:mm:ss Z yyyy"
        locale java.util.Locale/US
        sdf    (SimpleDateFormat. format locale)]
    (.setTimeZone sdf (java.util.TimeZone/getTimeZone "Asia/Tokyo"))
    (.parse sdf timestamp)))

(defn make-fs-tweet [tweet]
  (let [created_at (parse-twitter-timestamp (:created_at tweet))
        user       (:user tweet)]
    {"status_id"  (:id_str tweet)
     "user_id"    (:id_str user)
     "text"       (:text tweet)
     "created_at" created_at
     "updated_at" created_at}))

(defn tweet [{:keys [text]}]
  (let [result   (private/create-tweet twitter-auth proxies text)
        data     (make-fs-tweet result)
        tweet-id (:id_str result)]
    (try
      (log/info (str "post tweet completed. id=" tweet-id))
      (-> posts
          (.document tweet-id)
          (.set data))
      result
      (catch Exception e (log/error "post tweet Failed." (.getMessage e))))))

(defn tweet-random [{:keys [db]}]
  (let [data                               (pick-random)
        {content :content, author :author} data
        status                             (make-status data)]
    (tweet {:text status})))

(defn tweet-morning
  [{:keys [_]}]
  (tweet {:text "おはようございます"}))

(defn tweet-evening [{:keys [_]}]
  (tweet {:text "おはようございます"}))

;;;;;;;;;;;;;;;;;;;;
;; Design Journals
;;;;;;;;;;;;;;;;;;;;
(require '[clojure.pprint :as pp])

(defn dummy [{:keys [text screen_name db] :as params}]
  (println params)
  {:text        text
   :screen-name screen_name})

(comment

  (def tweet (private/get-tweet twitter-auth proxies "1500694005259980800"))
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
