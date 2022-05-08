(ns tools.dmm
  (:require
   [clojure.string :as string]
   [devtools :refer [env kotori-info]]
   [firebase :refer [db-dev db-prod]]
   [integrant.repl.state :refer [config system]]
   [kotori.domain.dmm.core :as model]
   [kotori.domain.dmm.product :as product]
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.product :as lib]
   [kotori.lib.provider.dmm.public :as public]
   [kotori.procedure.dmm.product :as dmm]
   [kotori.procedure.strategy.dmm :as st]
   [kotori.procedure.tweet.post :as post]))

(defn dmm-creds []
  (-> system
      (get :kotori.service.env/env)
      (api/env->creds)))

(defn creds [] (dmm-creds))

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

(defn get-dmm-product [cid floor]
  (if (= floor "anime")
    (lib/get-anime {:cid cid :creds (creds)})
    (lib/get-videoa {:cid cid :creds (creds)})))
#_(get-dmm-product "ssis00337")

(defn get-dmm-campaign [title]
  (lib/get-products {:limit 10 :keyword title :creds (creds)}))
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

(defn get-product
  [{:keys [env] :as m :or {floor (:videoa api/floor)}}]
  (let [creds (api/env->creds env)
        q     (dissoc m :env)]
    (-> (api/search-product creds q) first)))

(defn get-floors []
  (-> (api/get-floors creds) :site))

(defn download-floors! []
  (let [file-path "dmm/floor.edn"]
    (->> (get-floors)
         (io/dump-edn! file-path))))
#_(download-floors!)

(defn get-fanza-digital-floors []
  (-> (get-floors)
      second
      :service
      first
      :floor))

(defn video-floors->map [floors]
  (reduce (fn [acc m]
            (let [code (keyword (:code m))
                  id   (Integer/parseInt (:id m))]
              (assoc acc code id))) {} floors))

(def video-floor-map
  (delay (-> (get-fanza-digital-floors) (video-floors->map))))

(defn- af-url->list-url [url]
  (-> url
      (string/split #"&af_id")
      first
      (string/split #"lurl?=")
      second
      java.net.URLDecoder/decode))

(defn get-genres [floor-name]
  (let [q {:floor_id (floor-name @video-floor-map)
           :hits     500}]
    (->> (api/search-genre creds q)
         :genre
         (map (fn [m]
                (if-let [list-url (:list_url m)]
                  (assoc m :list_url (af-url->list-url list-url))
                  m)))
         (map (fn [m]
                (assoc m :genre_id (Integer/parseInt (:genre_id m)))))
         (sort-by :genre_id))))

(defn download-genres! [floor-name]
  (let [floor-name-str (name floor-name)
        file-path      (str "dmm/genre/" floor-name-str ".edn")]
    (->> (get-genres floor-name)
         (io/dump-edn! file-path))))
#_(download-genres! :videoa)

(defn download-all-genres! []
  (doseq [key (keys @video-floor-map)]
    (download-genres! key)))
#_(download-all-genres!)

;; 主に最新のdescを取得してちゃんとdescriptionをパースできているか
;; 確認するためのツール.
(defn scrape-descs [floor limit]
  (let [cids (->> (lib/get-products {:creds (creds)
                                     :floor floor
                                     :limit limit})
                  (map :content_id)
                  (into []))]
    (-> (public/get-page-bulk cids floor))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (def cid "h_454dhil10279")
  (def resp (get-dmm-product cid "anime"))
  (prn resp)
  )

(comment
  (def resp (get-floors))
  (get-genres :videoa)
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

(comment
  (def resp (scrape-descs "videoc" 10))
  (def descs (map :description resp))
  )
