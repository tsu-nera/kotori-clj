(ns kotori.lib.twitter.guest
  (:require
   [cheshire.core :as json]
   [clj-http.client :as client]
   [kotori.lib.twitter.config :refer [guest-bearer-token user-agent]]))

(def guest-token
  (delay
   (let [response (client/post
                   "https://api.twitter.com/1.1/guest/activate.json"
                   {:cookie-policy :none
                    :headers       {:authorization (str "Bearer " guest-bearer-token)
                                    :user-agent    user-agent}})]
     (-> response
         :body
         (json/parse-string true)
         :guest_token
         (or (throw (ex-info "Can't get guest token" {:response response})))))))

(def guest-headers
  {:headers
   {:authorization (str "Bearer " guest-bearer-token)
    :user-agent    user-agent
    :x-guest-token @guest-token}})

(defn get-user
  ([name]
   (let [url  (str "https://api.twitter.com/1.1/users/show.json?screen_name=" name)
         resp (client/get url guest-headers)]
     (-> resp
         :body
         (json/parse-string true)))))

(defn resolve-user-id
  [name]
  (:id_str (get-user name)))

(defn get-conversation-url [id]
  (str "https://api.twitter.com/2/timeline/conversation/"
       id
       ".json?include_reply_count=0&send_error_codes=true&count=20"))

;; 指定されたIDのツイートとそれにつながる全てのツイートが取れる
(defn get-conversation [id]
  (let [url  (get-conversation-url id)
        resp (client/get url guest-headers)]
    (-> resp
        :body
        (json/parse-string true))))

(defn get-tweet [id]
  (let [resp   (get-conversation id)
        id-key (keyword id)]
    (-> resp
        :globalObjects
        :tweets
        (id-key))))

;;;;;;;;;;;;;;;;
;; Debug Tools
;;;;;;;;;;;;;;;;
(comment
  (def user (get-user "richhickey"))
  (resolve-user-id "richhickey"))

(comment
  (def response (get-conversation "1477034578875277316"))
  (def response (get-tweet "1477034578875277316"))

  response
  )

;;;;;;;;;;;;;;;;;;;;
;; Design Journals
;;;;;;;;;;;;;;;;;;;;

;; (keys response)
;; ;; => (:cached
;;     :request-time
;;     :repeatable?
;;     :protocol-version
;;     :streaming?
;;     :http-client
;;     :chunked?
;;     :cookies
;;     :reason-phrase
;;     :headers
;;     :orig-content-encoding
;;     :status
;;     :length
;;     :body
;;     :trace-redirects)

;; (:status response);; => 200
;; ;; (:body response)
;; (def body (cheshire.core/parse-string (:body response) true))

;; (keys body)
;; ;; => ("globalObjects" "timeline")
;; (keys (:globalObjects body))

;; (pprint body)

;; (def tweets (:tweets (:globalObjects body)))

;; (count tweets)
;; (def tweets (vals tweets))

;; (def tweet (first tweets))

;; (defn get-text
;;   [tweet]
;;   (:text tweet))

;; (get-text tweet)

;; (map get-text tweets)
;; => ("@natalie_store COMING SOON？？？"
;;     "【謹賀新年】\nキタキタキタキタ福がキタ。\n新年を祝うキタキタ踊りですぞ〜‼︎\n\n#魔法陣グルグル #お正月 #めでたいものはなんですぞ #私の誕生日 https://t.co/IKqfWFFzbd"
;;     "@natalie_store @comic_natalie @Api_Apichua 😁"
;;     "@natalie_store @magical_grgr ＃めでたいものはなんですぞ　ってタグがあったことに笑ってしまいましたWWWW\nとりあえずツッコんでおきますね。\n\n「それはおめえの頭の中だあああああ！！！WWW」（12巻23ページ参照　笑）\n\nふうっ！(笑)")

;; (map #(:created_at %) tweets)
;; => ("Sat Jan 01 06:19:27 +0000 2022"
;;     "Fri Dec 31 21:51:00 +0000 2021"
;;     "Sat Jan 01 07:34:18 +0000 2022"
;;     "Sat Jan 01 04:24:51 +0000 2022")

;; https://joaoptrindade.com/clojure-tutorial-part-1-http-requests
;; (clojure.string/upper-case "hello")
;;
;; https://twitter.com/natalie_store/status/1477034578875277316

;;;; (defn get-status [status-id]
;;   (println "Get Status: id="status-id))

;; (defn get-status [status-id]
;;   {:id status-id})

;; (get-status "1234")

;;
;; code reading
;;
;; ref: https://github.com/vlaaad/tweet-def/blob/main/src/io/github/vlaaad/tweet_def.clj
;; (def bearer
;;   (delay
;;     (let [response (client/request
;;                     {:method :get
;;                      :cookie-policy :none
;;                      :url "https://abs.twimg.com/responsive-web/client-web/main.90f9e505.js"})]
;;       (or (->> response
;;                :body
;;                (re-find #"s=\"(AAAAA[^\"]+)\"")
;;                second)
;;           ;; M-x clojure-unwind-allで展開, 1つずつ読んでいく.
;;           ;;<- (second (re-find #"s=\"(AAAAA[^\"]+)\"" (:body response)))
;;           ;;
;;           ;; responseはmap
;;           ;; (:body responseで key=bodyのvalueを取り出す)
;;           ;; re-findで正規表現マッチを取り出し.
;;           ;; secondで2つ目を取り出し.
;;           (throw (ex-info "Can't get auth bearer" {:response response}))
;;           ;; throwで例外を投げる.
;;           ;; ex-infoで例外オブジェクトをつくる.
;;           ))))


;; (def guest-token
;;   (delay
;;     (let [response (client/request
;;                     {:method :post
;;                      :cookie-policy :none
;;                      :headers {"Authorization" (str "Bearer " guest-bearer-token)}
;;                      :url "https://api.twitter.com/1.1/guest/activate.json"})]
;;       (-> response
;;           :body
;;           (json/read-str :key-fn keyword)
;;           :guest_token
;;           (or (throw (ex-info "Can't get guest token" {:response response})))))))
