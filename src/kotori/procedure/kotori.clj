(ns kotori.procedure.kotori
  (:require
   [kotori.domain.kotori :as d]
   [kotori.domain.source.meigen :refer [meigens]]
   [kotori.domain.tweet.post :as post]
   [kotori.lib.firestore :as fs]
   [kotori.lib.twitter.private :as private]
   [kotori.procedure.strategy :as st]))

(defn pick-random []
  (rand-nth meigens))

(defn make-text [data]
  (let [{content :content, author :author} data]
    (str content "\n\n" author)))

(defn make-info [{:keys [screen-name user-id auth-token ct0]}]
  (let [creds (d/->Creds auth-token ct0)]
    (d/->Info screen-name user-id creds {})))

(defn tweet [{:keys [^d/Info info db text]}]
  (let [{:keys [user-id creds proxies]}
        info
        result   (private/create-tweet creds proxies text)
        tweet-id (:id_str result)
        doc-path (post/->doc-path user-id tweet-id)
        data     (post/->data result)]
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

(defn make-qvt-text [db]
  (let [product (st/select-next-qvt-product {:db db})
        url     (:url product)
        message "やべーよ!"]
    (str message "\n" url)))

(defn tweet-with-quoted-video
  "動画引用ツイート"
  [{:keys [db] :as params}]
  (let [text (make-qvt-text db)]
    (tweet (assoc params :text text))))

(defn tweet-morning
  [{:as params}]
  (tweet (assoc params :text "おはようございます")))

(defn tweet-evening
  [{:as params}]
  (tweet (assoc params :text "今日もお疲れ様でした")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn dummy [{:keys [^d/Info info db text]}]
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

  (tweet-with-quoted-video params)
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
