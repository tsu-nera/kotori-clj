(ns kotori.lib.provider.dmm.public
  (:require
   [clj-http.client :as client]
   [clojure.string :as str]
   [kotori.domain.dmm.core :as d]
   [kotori.lib.config :refer [user-agent]]
   [kotori.lib.provider.dmm.core :refer [request-bulk]]
   [kotori.lib.provider.dmm.parser :as p]
   [net.cgrand.enlive-html :as html]))

(def headers
  {:user-agent user-agent
   :cookie     "age_check_done=1"})

(defn uri-exists? [uri]
  (if-let [resp (client/head uri {:headers          headers
                                  :throw-exceptions false})]
    (= (:status resp) 200)
    false))

(defn cid->vr-uri [cid]
  (format "https://cc3001.dmm.co.jp/vrsample/%.1s/%.3s/%s/%svrlite.mp4"
          cid cid cid cid))

(defn get-page-raw
  ([url]
   ;; getの中でtimeout(socket/connection/connection-request)を設定しても
   ;; タイムアウトが効かないのでheadでページの存在をチェック.
   (when (uri-exists? url)
     (-> (client/get url {:headers headers})
         :body
         html/html-snippet)))
  ([cid floor]
   (let [url (d/->url floor cid)]
     (get-page-raw url))))

(defn ->title
  "APIで取得できる内容にあわせてサブタイトルはカット, 女優名は残す"
  [m]
  (-> m
      (html/select [:title])
      first
      :content
      first
      (str/split #"-")
      first
      str/trim))

;; 一応実際の - の数よりも少なくしておく
(defn- cut-underline [text]
  (let [re-line #"-------------------------------------"]
    (cond-> text
      (not (nil? text)) (str/split re-line)
      true              first)))

(defn ->raw-description [m]
  (-> m
      (html/select [(html/attr= :name "description")])
      first
      :attrs
      :content))

(defn- remove-fanza-headline [text]
  (if text
    (str/replace text #"【FANZA\(ファンザ\)】" "")
    text))

;; 自分でゴニョゴニョするのを諦めた.
;; https://stackoverflow.com/questions/3607965/how-to-convert-html-text-to-plain-text
(defn html->plain-text [html]
  (let [re (re-pattern "(?s)<[^>]*>(\\s*<[^>]*>)*")]
    (str/replace html re "")))

(defn replace-sharp [html]
  (let [re (re-pattern "＃")]
    (-> html
        (str/replace re " ＃"))))

(defn ->description [m]
  (let [raw (->raw-description m)]
    (-> raw
        html->plain-text
        replace-sharp
        remove-fanza-headline
        cut-underline)))

(defn get-page [{:keys [cid floor] :or {floor (:videoa d/floor)}}]
  (let [url (d/->url floor cid)]
    (when-let [m (get-page-raw url)]
      {:cid         cid
       :url         url
       :title       (->title m)
       :description (->description m)})))

(defn get-page-bulk [cids floor]
  (->> cids
       (map (fn [cid] {:cid cid :floor floor}))
       (request-bulk get-page)
       (into [])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;; 一応苦労して書いたから残しておくか...
  (defn- desc->aseq [text]
    (let [re (re-pattern "<a(.+?)a>")]
      (->> text
           (re-seq re)
           (map first))))

  (defn- ->tag-sentences-pair [text tag]
    (let [re (re-pattern
              (str "<" tag ">(.+?)</" tag ">"))]
      (re-seq re text)))

  (defn- remove-html-tag [tag text]
    (let [tags-pair (->tag-sentences-pair text tag)]
      (reduce (fn [text pair]
                (-> text
                    (str/replace
                     (first pair)
                     (second pair)))) text tags-pair)))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (def cid "h_1558csdx00007")
  (def url (d/->url "videoa" cid))
  (def data (get-page-raw cid "videoa"))

  (def title (->title data))
  (def description (->description data))

  (def content
    (-> data
        (html/select [(html/attr= :name "description")])
        first
        :attrs
        :content
        ))
  content

  (def cid "shinki066")
  (def resp (get-page {:cid cid :floor "videoc"}))

  (def data (get-page-raw cid "videoc"))
  (def raw-desc (->raw-description data))
  (def description (->description data))

  (def hastags (p/->hashtags description))
  )

(comment


  (def cid "oba00186")
  (def url "https://www.dmm.co.jp/digital/videoa/-/detail/=/cid=oba00186/")
  (def data (get-page-raw cid "videoa"))
  (def page (get-page {:cid cid}))

  (def resp (client/get url {:headers                    headers
                             :socket-timeout             1000
                             :connection-timeout         1000
                             :connection-request-timeout 1000
                             }))

  )

(comment
  (def cid "sivr00208")

  (defn uri-exists? [url]
    (if-let [resp (client/head url {:headers          headers
                                    :throw-exceptions false})]
      (= (:status resp) 200)
      false))

  (def uri (cid->vr-uri cid))
  (def ret (uri-exists? uri))
  )
