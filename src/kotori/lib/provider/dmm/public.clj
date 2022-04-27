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

(defn ->description [m]
  (-> m
      (html/select [(html/attr= :name "description")])
      first
      :attrs
      :content
      (str/split #"<br>")
      first
      (str/split #"】")
      rest
      first))

(defn get-page [cid]
  (let [m     (get-page-data cid)
        title (->title m)
        desc  (->description m)]
    {:cid cid :title title :description desc}))

(comment

  (def cid "pfes00034")
  (def url (domain/->url cid))
  (def page (get-page {:cid cid}))

  (def title (->title page))
  (def description (->description page))
  )
