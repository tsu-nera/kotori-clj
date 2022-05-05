(ns tools.dmm
  (:require
   [clojure.string :as string]
   [devtools :refer [env kotori-info]]
   [firebase :refer [db-dev db-prod]]
   [integrant.repl.state :refer [config system]]
   [kotori.domain.dmm.core :as model]
   [kotori.domain.dmm.floor :as floor]
   [kotori.domain.dmm.product :as product]
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.procedure.dmm.product :as dmm]
   [kotori.procedure.strategy.dmm :as st]
   [kotori.procedure.tweet.post :as post]))

(defn dmm-creds []
  (-> system
      (get :kotori.service.env/env)
      (api/env->creds)))

(defn make-dmm-tweet [screen-name post]
  {:cid         (:cid post)
   :media-id    (:media-id post)
   :media-key   (:media-key post)
   :screen-name screen-name
   :text        (:text post)
   :tweet-id    (:tweet-id post)
   :tweet-link  (:tweet-link post)
   :tweet-time  (:created-at post)
   :user-id     (:user-id post)})

(defn ->tweet [screen-name data]
  (let [cid        (:cid data)
        tweet-id   (:tweet-id data)
        tweet-time (:tweet-time data)
        tweets-key (fs/make-nested-key
                    ["tweets" screen-name tweet-id])]
    {"cid"             cid
     "last_tweet_name" screen-name
     "last_tweet_time" tweet-time
     "last_tweet_id"   tweet-id
     tweets-key        data}))

(defn ->path [data]
  (str product/coll-path "/" (:cid data)))

(defn- update-with-recovery! [db path post]
  (let [params (select-keys post
                            ["cid"
                             "last_tweet_name"
                             "last_tweet_time"
                             "last_tweet_id"])
        tweet  (dissoc post
                       "cid" "last_tweet_name"
                       "last_tweet_time" "last_tweet_id")]
    (if (fs/doc-exists? db path)
      (if (fs/doc-field-exists? db path "last_tweet_time")
        ;; last_tweet_timeが存在しているならば上書きはしない.
        (fs/update! db path tweet)
        (fs/update! db path post))
      (doto db
        (fs/set! path params)
        (fs/update! path tweet)))))

(defn assoc-post [db screen-name post]
  (->> post
       (make-dmm-tweet screen-name)
       ((juxt ->path
              #(->tweet screen-name %)))
       ((fn [[path tweet]]
          (update-with-recovery! db path tweet)))))

(defn assoc-posts [db screen-name posts]
  (->> posts
       (map #(make-dmm-tweet screen-name %))
       (map (juxt ->path
                  #(->tweet screen-name %)))
       (map (fn [[path tweet]]
              (update-with-recovery! db path tweet)))))

(defn qvt-without-summary->cids->file! [db limit file-path]
  (let [cids (map
              :cid (dmm/get-qvts-without-summary
                    {:db db :limit limit}))]
    (io/dump-str! file-path (string/join "\n" cids))))

(defn ->dmm-url [cid]
  (model/->url cid))

(defn get-dmm-product [cid & floor]
  (dmm/get-product {:env (env) :cid cid :floor floor}))
#_(get-dmm-product "ssis00337")

(defn get-dmm-campaign [title]
  (dmm/get-products {:env (env) :limit 10 :keyword title}))
#_(get-dmm-campaign "新生活応援30％OFF第6弾")

(defn crawl-product!
  ([cid]
   (crawl-product! cid (db-dev)))
  ([cid db]
   (dmm/crawl-product! {:db db :cid cid :env (env)})))

(defn metadata->csv-from-fs! [db limit csv-path]
  (dmm/crawl-qvt-descs! {:db db :limit limit})
  (let [qvts (dmm/get-qvts-without-summary {:db db :limit limit})
        maps (->> qvts
                  (map #(select-keys % [:cid :title :description]))
                  (filter #(:description %)))]
    (io/dump-csv-from-maps! csv-path maps)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (def cid "196glod00227")
  (def resp (get-dmm-product cid "anime"))
  )

(comment
  (metadata->csv-from-fs! (db-prod) 300 "tmp/metas.csv")
  )

(comment
  (def info (kotori-info "0001"))
  (def screen-name (:screen-name info))

  (def products (st/select-tweeted-products
                 {:db (db-prod) :screen-name screen-name :limit 100}))

  (def qvts (dmm/get-qvts-without-summary {:db (db-prod) :limit 100}))
  (def targets (->> qvts
                    (map #(select-keys % [:cid :title :description]))
                    (filter #(:description %))))
  (io/dump-csv-from-maps! "tmp/metas.csv" targets)

  (def cids (dmm/get-products-by-cids {:cids cids :env (env)}))
  (def result (dmm/crawl-products-by-cids! {:cids cids
                                            :db   (db-prod) :env (env)}))
  )

(comment  ;;;
  (require '[firebase :refer [db-dev db-prod]]
           '[devtools :refer [kotori-info]])

  (def info (kotori-info "0001"))
  (def user-id (:user-id info))
  (def screen-name (:screen-name info))

  (def resp (post/get-video-posts {:db       (db-prod)
                                   :user-id  user-id
                                   :days-ago 35
                                   :days     7}))
  (count resp)
  (assoc-posts (db-dev) screen-name resp)

    ;;;;;;;;;;;;;;;
  (def post (second resp))
  (assoc-post (db-dev) screen-name post)
  ;;;;;;;;;;;;;;;;;;;;;;

  (def product (dmm/crawl-product!
                {:db (db-dev) :env (env) :cid "mide00990"}))

  (def result (crawl-product! "mide00897"))

  (def products (dmm/crawl-products!
                 {:db (db-prod) :env (env) :limit 100}))
 ;;;
  )
