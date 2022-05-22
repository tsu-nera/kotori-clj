(ns tools.dmm
  (:require
   [clojure.java.browse :as b]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [clojure.string :as string]
   [defun.core :refer [defun]]
   [devtools :refer [env kotori-info]]
   [firebase :refer [db-dev db-prod]]
   [integrant.repl.state :refer [config system]]
   [kotori.domain.dmm.core :as d]
   [kotori.lib.firestore :as fs]
   [kotori.lib.io :as io]
   [kotori.lib.kotori :as kotori]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.doujin :as doujin]
   [kotori.lib.provider.dmm.ebook :as ebook]
   [kotori.lib.provider.dmm.product :as product]
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

(defn- ->path [data]
  (str "providers/dmm/products" (:cid data)))

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
  (d/->url cid))

(defun get-dmm
  "DMM APIから取得"
  ([cid "anime"] (product/get-anime {:cid cid :creds (creds)}))
  ([cid "videoc"] (product/get-videoc {:cid cid :creds (creds)}))
  ([cid "videoa"] (product/get-videoa {:cid cid :creds (creds)}))
  ([cid "comic"] (ebook/get-comic {:cid cid :creds (creds)}))
  ([cid "digital_doujin"] (doujin/get-doujin {:cid cid :creds (creds)}))
  ([cid] (product/get-videoa {:cid cid :creds (creds)})))
#_(get-dmm "ssis00337")

(defn ->product-dump-path [service floor cid]
  (str "dmm/product/" service "/" floor "/" cid ".edn"))

(defn remove-headdesc [text]
  (let [re (re-pattern "^--------------(.+)--------------|^//////////////(.+)//////////////")]
    (if-let [target (first (re-find re text))]
      (-> text
          (str/replace target ""))
      text)))

(defn ->af-id-removed [url]
  (let [re (re-pattern "&af_id=(.+?)&")]
    (if-let [target (second (re-find re url))]
      (-> url
          (str/replace target "hogehoge-xxx"))
      url)))

(defn dump-product! [service floor cid]
  (let [file-path (->product-dump-path service floor cid)]
    (when-let [m (get-dmm cid floor)]
      (let [data (cond-> m
                   (:affiliateURL m)
                   (update :affiliateURL ->af-id-removed)
                   (get-in m [:URL :affiliateURL])
                   (update-in [:URL :affiliateURL] ->af-id-removed))]
        (io/dump-example-edn! file-path data)))))

(defn dump-doujin! [cid]
  (dump-product! "doujin" "digital_doujin" cid))

(defn dump-comic! [cid]
  (dump-product! "ebook" "comic" cid))

(defn get-dmm-campaign [title]
  (product/get-products {:limit 10 :keyword title :creds (creds)}))
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

;; 主に最新のdescをAPI経由で取得して
;; ちゃんとdescriptionをパースできているか確認するためのツール.
(defn scrape-descs [floor limit]
  (let [cids (->> (product/get-products {:creds (creds)
                                         :floor floor
                                         :limit limit})
                  (map :content_id)
                  (into []))]
    (-> (public/get-page-bulk cids floor))))

(defn ->videoa-url [cid]
  (d/->url "videoa" cid))

(defn ->videoc-url [cid]
  (d/->url "videoc" cid))

(defn ->anime-url [cid]
  (d/->url "anime" cid))

(defn open-dmm
  ([cid]
   (b/browse-url (d/->url "videoa" cid)))
  ([cid floor]
   (b/browse-url (d/->url floor cid))))

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
  (def resp (scrape-descs "videoc" 20))
  (def descs (map :description resp))
  (def descs2 (map kotori/desc->trimed  descs))

  (def page (nth resp 6))
  )

(comment
  (dump-comic! "b104atint00851")
  )
