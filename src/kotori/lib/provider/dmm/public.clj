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

(defn get-page-data
  ([url]
   (-> (client/get url {:headers headers})
       :body
       html/html-snippet))
  ([cid floor]
   (let [url (d/->url cid floor)]
     (get-page-data url))))

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

(defn- desc->aseq [text]
  (let [re (re-pattern "<a(.+?)a>")]
    (->> text
         (re-seq re)
         (map first))))

(defn- remove-aseq [text]
  (let [aseq (desc->aseq text)]
    (reduce (fn [text a]
              (-> text
                  (str/replace a "")
                  (str/replace #"「」" ""))) text aseq)))

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

(defn- lines-two->one [text]
  (str/replace text #"<br> <br>" "<br>"))

(defn- remove-bold-tag [text]
  (-> text
      (str/replace #"<b>" "")
      (str/replace #"</b>" "")))

(defn- remove-big-tag [text]
  (-> text
      (str/replace #"<big>" "")
      (str/replace #"</big>" "")))

(defn- remove-span-tag [text]
  (-> text
      (str/replace #"</span>" "")
      (str/replace #"<span" "")
      (str/replace #"style=" "")
      (str/replace #"\"color:red\">" "")
      (str/replace #"\"color:blue\">" "")))

(defn- remove-br-tag [text]
  (-> text
      (str/replace #"<br>" " ")
      (str/replace #"<br />" " ")
      ;; (str/replace #" " "")
      ))

;; 文末に注意書きがあることがおおいのでtrimしておく.
;; 文中をtrimしないように処理の最後に呼ぶ.
;; 文中のアスタリスクがうまく裁けないので廃止.
#_(defn- remove-last-asterisk [text]
    (-> text
        (str/split #"※")
        first))

(defn ->description [m]
  (let [raw (->raw-description m)]
    (-> raw
        remove-fanza-headline
        cut-underline
        lines-two->one
        remove-big-tag
        remove-bold-tag
        remove-span-tag
        remove-br-tag
        remove-aseq
        p/->remove-hashtags)))

(defn get-page [{:keys [cid floor] :or {floor (:videoa d/floor)}}]
  (let [url   (d/->url cid floor)
        m     (get-page-data url)
        title (->title m)
        desc  (->description m)]
    {:cid cid :title title :description desc :url url}))

(defn get-page-bulk [cids floor]
  (->> cids
       (map (fn [cid] {:cid cid :floor floor}))
       (request-bulk get-page)
       (into [])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (def cid "h_1558csdx00007")
  (def url (d/->url cid "videoa"))
  (def data (get-page-data cid "videoa"))

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

  (def cid "ipx00850")
  (def resp (get-page {:cid cid :floor "video"}))

  (def data (get-page-data "apns00067" "videoa"))
  (def description (->description data))

  (def hastags (p/->hashtags description))

  (-> (->raw-description data)
      remove-fanza-headline
      cut-underline
      lines-two->one
      remove-bold-tag
      remove-big-tag
      remove-span-tag
      remove-br-tag
      remove-aseq
      p/->remove-hashtags
      )
  )
