(ns kotori.domain.dmm.product
  (:require
   [clojure.string :as string]
   [kotori.lib.json :as json]
   [kotori.lib.time :as time]))

(defn- ->cid [raw]
  (:content_id raw))

(defn- ->title [raw]
  (:title raw))

(defn- ->url [raw]
  (:URL raw))

(defn- ->affiliate-url [raw]
  (:affiliateURL raw))

(defn- ->actresses [raw]
  (get-in raw [:iteminfo :actress]))

(defn- ->performer [raw]
  (string/join
   "," (map #(:name %) (->actresses raw))))

(defn- ->released-time [raw]
  (let [date-str (:date raw)]
    (-> date-str
        (time/parse-dmm-timestamp)
        (time/->fs-timestamp))))

(defn- ->genres [raw]
  (get-in raw [:iteminfo :genre]))

(defn- ->genre [raw]
  (string/join
   "," (map #(:name %) (->genres raw))))

(defn- ->timestamp [_] (time/->fs-timestamp (time/now)))

(defn ->legacy [raw]
  {:cid         (->cid raw)
   :title       (->title raw)
   :url         (->affiliate-url raw)
   :performer   (->performer raw)
   :released_at (->released-time raw)
   :created_at  (->timestamp raw)
   :updated_at  (->timestamp raw)
   :genre       ()
   :status      "READY"
   :ranking     0
   :posted_at   nil
   :tweet_link  nil
   :tweet_id    nil})

(defn ->data [raw]
  "dmm response map -> firestore doc mapの写像"
  (let [data   {:cid           (->cid raw)
                :title         (->title raw)
                :url           (->url raw)
                :affiliate_url (->affiliate-url)
                :actresses     (->actresses raw)
                :released_time (->released-time raw)
                :genres        (->genres raw)
                :created_time  (->timestamp raw)
                :updated_time  (->timestamp raw)}
        legacy (->legacy raw)]
    (-> data
        (assoc :raw raw)
        (assoc :legacy legacy)
        (json/->json))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[local :refer [env db]])
  (require '[kotori.procedure.dmm :refer [get-product]])
  (def raw
    (get-product {:cid "ssis00335" :env (env)}))

  (def actresses (->actresses raw))
  (map #(:name %) actresses)

  (def data (->data raw))

  )

(comment
  (require '[portal.api :as p])
  (def d (p/open))
  (reset! d (->data (get-product {:cid "ssis00335" :env (env)})))
  @d
  )
;; => {:cid "ssis00337",
;;     :raw
;;     {:imageURL
;;      {:list "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337pt.jpg",
;;       :small "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337ps.jpg",
;;       :large "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337pl.jpg"},
;;      :date "2022-03-18 10:00:59",
;;      :sampleImageURL
;;      {:sample_s
;;       {:image
;;        ["https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337-1.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337-2.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337-3.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337-4.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337-5.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337-6.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337-7.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337-8.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337-9.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337-10.jpg"]},
;;       :sample_l
;;       {:image
;;        ["https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337jp-1.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337jp-2.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337jp-3.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337jp-4.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337jp-5.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337jp-6.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337jp-7.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337jp-8.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337jp-9.jpg"
;;         "https://pics.dmm.co.jp/digital/video/ssis00337/ssis00337jp-10.jpg"]}},
;;      :content_id "ssis00337",
;;      :service_name "動画",
;;      :floor_code "videoa",
;;      :product_id "ssis00337",
;;      :iteminfo
;;      {:genre
;;       [{:id 6533, :name "ハイビジョン"}
;;        {:id 79015, :name "4K"}
;;        {:id 6548, :name "独占配信"}
;;        {:id 5019, :name "パイズリ"}
;;        {:id 6017, :name "ギリモザ"}
;;        {:id 2001, :name "巨乳"}
;;        {:id 4025, :name "単体作品"}],
;;       :series [{:id 78603, :name "新人NO.1 STYLE"}],
;;       :maker [{:id 3152, :name "エスワン ナンバーワンスタイル"}],
;;       :actress [{:id 1074740, :name "うんぱい", :ruby "うんぱい"}],
;;       :director [{:id 101612, :name "紋℃", :ruby "もんど"}],
;;       :label [{:id 3474, :name "S1 NO.1 STYLE"}]},
;;      :affiliateURL
;;      "https://al.dmm.co.jp/?lurl=https%3A%2F%2Fwww.dmm.co.jp%2Fdigital%2Fvideoa%2F-%2Fdetail%2F%3D%2Fcid%3Dssis00337%2F&af_id=romanchikubi-992&ch=api",
;;      :floor_name "ビデオ",
;;      :title "エスワン専属女優になりました。うんぱいS1大型契約決定！！ たっぷり3時間ねっとり3本番",
;;      :volume "219",
;;      :category_name "ビデオ (動画)",
;;      :affiliateURLsp
;;      "https://al.dmm.co.jp/?lurl=https%3A%2F%2Fwww.dmm.co.jp%2Fdigital%2Fvideoa%2F-%2Fdetail%2F%3D%2Fcid%3Dssis00337%2F&af_id=romanchikubi-992&ch=api",
;;      :prices
;;      {:price "2480~",
;;       :list_price "2480~",
;;       :deliveries
;;       {:delivery
;;        [{:type "4k", :price "3680", :list_price "3680"}
;;         {:type "hd", :price "2980", :list_price "2980"}
;;         {:type "download", :price "2480", :list_price "2480"}
;;         {:type "stream", :price "2480", :list_price "2480"}
;;         {:type "iosdl", :price "2480", :list_price "2480"}
;;         {:type "androiddl", :price "2480", :list_price "2480"}]}},
;;      :review {:count 47, :average "2.83"},
;;      :sampleMovieURL
;;      {:size_476_306
;;       "https://www.dmm.co.jp/litevideo/-/part/=/cid=ssis00337/size=476_306/affi_id=romanchikubi-992/",
;;       :size_560_360
;;       "https://www.dmm.co.jp/litevideo/-/part/=/cid=ssis00337/size=560_360/affi_id=romanchikubi-992/",
;;       :size_644_414
;;       "https://www.dmm.co.jp/litevideo/-/part/=/cid=ssis00337/size=644_414/affi_id=romanchikubi-992/",
;;       :size_720_480
;;       "https://www.dmm.co.jp/litevideo/-/part/=/cid=ssis00337/size=720_480/affi_id=romanchikubi-992/",
;;       :pc_flag 1,
;;       :sp_flag 1},
;;      :URLsp "https://www.dmm.co.jp/digital/videoa/-/detail/=/cid=ssis00337/",
;;      :service_code "digital",
;;      :URL "https://www.dmm.co.jp/digital/videoa/-/detail/=/cid=ssis00337/"},
;;     :legacy {:cid "ssis00337"}}
