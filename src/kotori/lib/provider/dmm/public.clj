(ns kotori.lib.provider.dmm.public
  (:require
   [clj-http.client :as client]
   [clojure.string :as str]
   [kotori.domain.dmm.core :as domain]
   [kotori.lib.config :refer [user-agent]]
   [net.cgrand.enlive-html :as html]))

(def headers
  {:user-agent user-agent
   :cookie     "age_check_done=1"})

(defn get-page-data [cid]
  (let [url (domain/->url cid)]
    (-> (client/get url {:headers headers})
        :body
        html/html-snippet)))

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

(defn ->description [m]
  (-> m
      (html/select [(html/attr= :name "description")])
      first
      :attrs
      :content
      (str/split #"<br> <br>")
      first
      (str/split #"-------------------------------------")
      first
      (str/replace #"<br>" "")
      (str/replace #"<br />" "")
      (str/replace #"<b>" "")
      (str/replace #"</b>" "")
      (str/replace #"</span>" "")
      (str/replace #"<span" "")
      (str/replace #"style=" "")
      (str/replace #"\"color:red\">" "")
      (str/replace #"\"color:blue\">" "")
      (str/replace #"【FANZA\(ファンザ\)】" "")
      (str/replace #" " "")
      remove-aseq
      (str/split #"※")
      first))

(defn get-page [cid]
  (let [m     (get-page-data cid)
        title (->title m)
        desc  (->description m)]
    {:cid cid :title title :description desc}))

(comment
  (def cid "h_1558csdx00007")
  (def url (domain/->url cid))
  (def data (get-page-data cid))

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

  (def cid "mgdv00064")
  (def data (get-page-data cid))
  (def description (->description data))

  (def ret (-> description desc->aseq))
  (def ret (-> description remove-aseq))
  )
