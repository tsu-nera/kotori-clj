(ns kotori.lib.api.handler
  (:require
   [kotori.procedure.dmm :as dmm]
   [kotori.procedure.kotori :as kotori]
   [kotori.procedure.ping :as ping]
   [kotori.procedure.strategy :as strategy]
   [reitit.ring :as ring]
   [ring.util.response :as resp]))

(defn http->
  "HTTP Request Map -> Procedure Input Map."
  [req]
  (:params req))

(defn ->http
  "Procedure Output Map -> HTTP Response Map."
  [out]
  (resp/response out))

(defn wrap-http [handler]
  (fn [req]
    (-> req
        http->
        handler
        ->http)))

(def routes
  (ring/router
   ["/api"  {:middleware [#(wrap-http %)]}
    ["/ping" {:post ping/ping-pong}]
    ["/dmm"
     ["/crawl-product" {:post dmm/crawl-product!}]
     ["/crawl-products" {:post dmm/crawl-products!}]
     ["/select-next-product" {:get strategy/select-next-product}]]
    ["/kotori"
     ["/dummy" kotori/dummy]
     ["/tweet" kotori/tweet]
     ["/tweet-morning" kotori/tweet-morning]
     ["/tweet-evening" kotori/tweet-evening]
     ["/tweet-random" kotori/tweet-random]]]))

(defn make-endpoint []
  (ring/ring-handler routes))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(comment
  ((make-endpoint) {:request-method :post :uri "/api/ping"})
  ((make-endpoint) {:request-method :post :uri "/api/dmm/get-product"})
  ((make-endpoint) {:request-method :post :uri "/api/kotori/dummy"})
  ((make-endpoint) {:request-method :post :uri "/api/kotori/tweet-random"})
  )

(comment
  (def handler-resp
    {:contributors              nil,
     :coordinates               nil,
     :created_at                "Sat Mar 19 23:08:54 +0000 2022",
     :entities                  {:hashtags [], :symbols [], :urls [], :user_mentions []},
     :favorite_count            0,
     :favorited                 false,
     :geo                       nil,
     :id                        nil,
     :id_str                    "",
     :in_reply_to_screen_name   nil,
     :in_reply_to_status_id     nil,
     :in_reply_to_status_id_str nil,
     :in_reply_to_user_id       nil,
     :in_reply_to_user_id_str   nil,
     :is_quote_status           false,
     :lang                      "ja",
     :place                     nil,
     :retweet_count             0,
     :retweeted                 false,
     :source                    "<a href=\"https://mobile.twitter.com\" rel=\"nofollow\">Twitter Web App</a>",
     :supplemental_language     nil,
     :text                      "友よ、逆境にあっては、常にこう叫ばねばならない。「希望、希望、また希望」と。\n\nヴィクトル・ユーゴー",
     :truncated                 false})
  )

#_(assoc resp-ok :body {:test "hoge"})
#_(update resp-ok :body (constantly handler-resp))
#_(App {:request-method :post :uri "/api/tweet-evening"})
