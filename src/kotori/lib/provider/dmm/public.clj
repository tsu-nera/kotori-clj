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

(defn get-page [{:keys [cid]}]
  (let [url (domain/->url cid)]
    (-> (client/get url {:headers headers})
        :body)))

(defn ->title
  "APIで取得できる内容にあわせてサブタイトルはカット, 女優名は残す"
  [page]
  (-> page
      (html/html-snippet)
      (html/select [:title])
      first
      :content
      first
      (str/split #"-")
      first))

(comment
  (def cid "pfes00034")
  (def page (get-page {:cid cid}))
  (def title (->title page))
  )
