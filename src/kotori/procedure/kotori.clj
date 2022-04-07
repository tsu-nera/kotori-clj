(ns kotori.procedure.kotori
  (:require
   [kotori.domain.kotori :as kotori]
   [kotori.domain.meigen :refer [meigens]]
   [kotori.domain.tweet.post :refer [posts]]
   [kotori.lib.time :as time]
   [kotori.lib.twitter.guest :as guest]
   [kotori.lib.twitter.private :as private]))

(defn pick-random []
  (rand-nth meigens))

(defn make-text [data]
  (let [{content :content, author :author} data]
    (str content "\n\n" author)))

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
      (println (str "post tweet completed. id=" tweet-id))
      (-> posts
          (.document tweet-id)
          (.set data))
      result
      (catch Exception e (println "post tweet Failed." (.getMessage e))))))

(defn tweet-random [{:as params}]
  (let [data (pick-random)
        text (make-text data)]
    (tweet (assoc params :text text))))

(defn tweet-with-quoted-video
  "引用動画ツイート"
  [{:as params}]
  (let [text "TODO"]
    (tweet (assoc params :text text))))

(defn tweet-morning
  [{:as params}]
  (tweet (assoc params :text "おはようございます")))

(defn tweet-evening
  [{:as params}]
  (tweet (assoc params :text "お疲れ様です")))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn dummy [{:keys [text screen-name db]}]
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
  ;;;
  (require '[firebase :refer [db]]
           '[devtools :refer [env]])

  (def text (make-text (pick-random)))
  (tweet-random {:db (db)})

  (defn dtweet [{:keys [text screen-name db env]}]
    (let [screen-name (or screen-name (:screen-name env))
          user-id     (guest/resolve-user-id screen-name)
          ;; creds       (kotori/->creds db user-id)
          ;; proxies     (kotori/->proxies db user-id)
          ;; result      (private/create-tweet creds proxies text)
          ;; data        (make-fs-tweet result)
          ;; tweet-id    (:id_str result)
          ]
      ;; (try
      ;;   (log/info (str "post tweet completed. id=" tweet-id))
      ;;   (-> posts
      ;;       (.document tweet-id)
      ;;       (.set data))
      ;;   result
      ;;   (catch Exception e (log/error "post tweet Failed." (.getMessage e))))
      user-id
      )
    )

  (defn dtweet-with-quoted-video
    "引用動画ツイート"
    [{:as params}]
    (let [text "TODO"]
      (dtweet (assoc params :text text))))

  (dtweet-with-quoted-video {:db (db) :env (env)})
 ;;;
  )

(comment
  ;;;
  (require '[local :refer [twitter-auth]])
  (def tweet (private/get-tweet (twitter-auth) "1500694005259980800"))
  (def user (private/get-user (twitter-auth) "46130870"))
  (def resp (private/create-tweet (twitter-auth) "test"))

  (def status-id (:id_str resp))
  (def resp (private/delete-tweet (twitter-auth) status-id))
  ;;;
  )
