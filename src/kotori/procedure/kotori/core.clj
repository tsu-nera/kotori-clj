(ns kotori.procedure.kotori.core
  (:require
   [clojure.spec.alpha :as s]
   [kotori.domain.kotori :as d]
   [kotori.domain.source.meigen :as meigen]
   [kotori.domain.tweet.post :as post]
   [kotori.lib.firestore :as fs]
   [kotori.procedure.strategy.core :as st]
   [kotori.procedure.strategy.dmm :as st-dmm]
   [twitter-clj.private :as private]))

(defn make-info [{:keys [screen-name user-id auth-token ct0 proxy-map]}]
  (d/make-info screen-name user-id
               {:auth-token auth-token :ct0 ct0} proxy-map))

(defn make-text [source strategy builder]
  (builder (strategy source)))

(defn tweet [{:keys [^d/Info info db text type]}]
  (let [{:keys [user-id cred proxy]}
        info
        result   (private/create-tweet cred proxy text)
        tweet-id (:id_str result)
        doc-path (post/->doc-path user-id tweet-id)
        data     (post/->data result type)]
    (try
      (println (str "post tweet completed. id=" tweet-id))
      (fs/set! db doc-path data)
      result
      (catch Exception e
        (println "post tweet Failed." (.getMessage e))))))

(defn tweet-morning
  [{:as params}]
  (tweet (assoc params :text "おはようございます" :type :text)))

(defn tweet-evening
  [{:as params}]
  (tweet (assoc params :text "今日もお疲れ様でした" :type :text)))

(defn tweet-random [{:keys [^d/Info info db env] :as params}]
  (let [source       meigen/source
        strategy     st/pick-random
        text-builder meigen/build-text
        text         (make-text source strategy text-builder)]
    (tweet (assoc params :text text :type :text))))

(defn select-next-product [{:keys [db screen-name]}]
  {:pre [(s/valid? ::d/screen-name screen-name)]}
  (st-dmm/->next (first (st-dmm/select-scheduled-products
                         {:db db :screen-name screen-name}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn dummy [{:keys [^d/Info info db text]}]
  (assoc info :text text))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  ;;;
  (require '[firebase :refer [db db-prod db-dev]]
           '[devtools :refer [env kotori-info ->screen-name info-dev]])

  (def params {:db (db) :info @info-dev})

  ;;;;;;;;;;;;;
  (tweet-morning params)
  (tweet-evening params)
  (tweet-random params)

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
