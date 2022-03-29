(ns kotori.lib.twitter.private
  (:require
   [clj-http.client :as client]
   [kotori.lib.config :refer [user-agent]]
   [kotori.lib.twitter.config :refer [guest-bearer-token]]))

(def options {:decode-cookies false :cookie-policy :none})

(defn- parse-body
  [resp]
  (-> resp
      :body))

(defn creds->headers
  [{:keys [auth-token ct0]}]
  (let [cookie (str "auth_token=" auth-token "; ct0=" ct0)]
    {:cookie-policy :standard
     :as            :json
     :accept        :json
     :headers
     {:authorization (str "Bearer " guest-bearer-token)
      :user-agent    user-agent
      :x-csrf-token  ct0
      :cookie        cookie}}))

(defn proxy-port-string->number [m]
  (let [port (:proxy-port m)]
    (assoc m :proxy-port (int port))))

(defn get-tweet
  ([creds id]
   (get-tweet creds {} id))
  ([creds proxy id]
   (let [url     (str "https://api.twitter.com/2/timeline/conversation/"
                      id ".json?count=1")
         headers (creds->headers creds)
         params  (merge headers proxy options)
         id-key  (keyword id)
         resp    (client/get url params)]
     (-> resp
         parse-body
         :globalObjects
         :tweets
         id-key))))

(defn get-user
  ([creds id]
   (get-user creds {} id))
  ([creds proxy id]
   (let [url     (str "https://api.twitter.com/1.1/users/show.json?user_id=" id)
         headers (creds->headers creds)
         params  (merge headers proxy options)
         resp    (client/get url params)]
     (-> resp
         (parse-body)
         (dissoc :status)))))

(defn create-tweet
  ([creds text]
   (create-tweet creds {} text))
  ([creds proxy text]
   (let [url     "https://twitter.com/i/api/1.1/statuses/update.json"
         data    {:form-params {:status text}}
         headers (creds->headers creds)
         params  (merge data headers proxy options)
         resp    (client/post url params)]
     (-> resp
         (parse-body)
         ;; (dissoc :user)
         ))))

(defn delete-tweet
  ([creds id]
   (create-tweet creds {} id))
  ([creds proxy id]
   (let [url     "https://twitter.com/i/api/1.1/statuses/destroy.json"
         data    {:form-params {:id id}}
         headers (creds->headers creds)
         params  (merge data headers proxy options)
         resp    (client/post url params)]
     (-> resp
         (parse-body)
         (dissoc :user)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 関数の名前はtweepy参考にしようかな.
;; https://docs.tweepy.org/en/stable/client.html

(comment
  (require '[local :refer [twitter-auth]])
  (def user (get-user (twitter-auth) "46130870"))
  )

(comment
  ;; https://docs.tweepy.org/en/stable/client.html#tweepy.Client.create_tweet
  ;; tweepyは create_tweetになってる. statusという言葉はつかっていない.
  ;; これに従おうか.
  ;;
  ;;

  (defn delete-tweet
    ([creds id]
     (create-tweet creds {} id))
    ([creds proxy id]
     (let [url     "https://twitter.com/i/api/1.1/statuses/destroy.json"
           data    {:form-params {:id id}}
           headers (creds->headers creds)
           params  (merge data headers proxy options)
           resp    (client/post url params)]
       (-> resp
           (parse-body)))))


  (defn create-tweet
    ([creds text]
     (create-tweet creds {} text))
    ([creds proxy text]
     (let [url     "https://twitter.com/i/api/1.1/statuses/update.json"
           data    {:form-params {:status text}}
           headers (creds->headers creds)
           params  (merge data headers proxy options)
           resp    (client/post url params)]
       (-> resp
           :body))
     ))
  )

(comment

  (require '[clj-http.client :as client])
  client/*current-middleware*
  client/default-middleware

  )

(comment
  ;; (defrecord TwitterCreds [auth-token ct0])
  ;; (def creds (apply ->TwitterCreds auth))
  ;; (defrecord Proxy [host port user pass])

  (defn get-statuses [status-id]
    (let [url (str "https://api.twitter.com/2/timeline/conversation/"
                   status-id
                   ".json?include_reply_count=1&send_error_codes=true&count=20")]
      (client/get url headers)))

  (defn get-status [status-id]
    (let [response (get-statuses status-id)]
      (-> response
          :body
          (json/parse-string true)
          :globalObjects
          :tweets
          (get (keyword status-id)))))

  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; (def status (get-status "1477034578875277316"))
;; (def response (update-status "test5"))

;; 理想的にはREPL起動時のcore初期化をしてグローバルな参照を取得したい.
;; (def user-id (env :user-id))
;; (def db (get-fs))
;; (def auth (let [kotori-coll-path (str "kotoris" "/" user-id)
;;                 coll             (.doc db kotori-coll-path)
;;                 query            (.get coll)]
;;             (->>
;;              (.getDocuments @query)
;;              (map #(.getData %)))))

;; (def db (get-fs))
;; (def kotori-coll-path (str "kotoris" "/" user-id))
;; (def docref (.document db kotori-coll-path))
;; (def future (.get docref))
;; (def doc (.get future))
;; (def d (.getData doc))

;; そうか, このdataはjava.util.HashMapなのでひと工夫必要.
;; (def data (into {} d))
;; (def auth (get data "twitter_auth"))

;; やっと取れた.
;; auth

;; hashmapをkeywordにしたい.
;; kebabみたいなライブラリつかえないか？
;; firestoreのファイルにユーティリティををまとめたい.
;; 遅延評価を取り入れたい.
