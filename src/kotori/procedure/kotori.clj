(ns kotori.procedure.kotori
  (:require
   [kotori.domain.kotori :as d]
   [kotori.domain.meigen :refer [meigens]]
   [kotori.domain.tweet.post :refer [->doc-path]]
   [kotori.lib.firestore :as fs]
   [kotori.lib.time :as time]
   [kotori.lib.twitter.guest :as guest]
   [kotori.lib.twitter.private :as private]))

(defn pick-random []
  (rand-nth meigens))

(defn make-text [data]
  (let [{content :content, author :author} data]
    (str content "\n\n" author)))

(defn make-info [{:keys [screen-name user-id auth-token ct0]}]
  (let [creds (d/->Creds auth-token ct0)]
    (d/->Info screen-name user-id creds {})))

(defn make-fs-tweet [tweet]
  (let [created_at (time/parse-twitter-timestamp (:created_at tweet))
        user       (:user tweet)]
    {"status_id"  (:id_str tweet)
     "user_id"    (:id_str user)
     "text"       (:text tweet)
     "created_at" created_at
     "updated_at" created_at}))

(defn tweet [{:keys [^d/INFO info db text]}]
  (let [{:keys [user-id creds proxies]} info
        result                          (private/create-tweet creds proxies text)
        data                            (make-fs-tweet result)
        tweet-id                        (:id_str result)
        doc-path                        (->doc-path user-id tweet-id)]
    (try
      (println (str "post tweet completed. id=" tweet-id))
      (fs/set! db doc-path data)
      result
      (catch Exception e
        (println "post tweet Failed." (.getMessage e))))))

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
  (tweet (assoc params :text "今日もお疲れ様でした")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn dummy [{:keys [^d/INFO info db text]}]
  (assoc info :text text))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;;;
  (require '[firebase :refer [db]]
           '[devtools :refer [kotori-info]])

  (def params {:db (db) :info (kotori-info "0003")})

  (def text (make-text (pick-random)))
  (tweet-random params)
  (tweet-evening params)

 ;;;
  )

(comment
  ;;;
  (require '[devtools :refer [twitter-auth]])
  (def auth (twitter-auth))

  (def tweet (private/get-tweet (twitter-auth) "1500694005259980800"))
  (def user (private/get-user (twitter-auth) "46130870"))
  (def resp (private/create-tweet (twitter-auth) "test"))

  (def status-id (:id_str resp))
  (def resp (private/delete-tweet (twitter-auth) status-id))
  ;;;

  )
