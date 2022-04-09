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
  ([creds proxies text]
   (let [url     "https://twitter.com/i/api/1.1/statuses/update.json"
         data    {:form-params {:status text}}
         headers (creds->headers creds)
         params  (merge data headers proxies options)
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
  (require '[devtools :refer [twitter-auth]])
  (def user (get-user (twitter-auth) "46130870"))
  )

(comment
  (require '[devtools :refer [twitter-auth]])
  (def tweet (get-tweet (twitter-auth) ""))
  )

(comment

  (require '[clj-http.client :as client])
  client/*current-middleware*
  client/default-middleware

  )

