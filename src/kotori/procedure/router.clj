(ns kotori.procedure.router
  (:require
   [kotori.procedure.kotori :as kotori]
   [reitit.core :as r]
   [reitit.ring :as ring]))

(def routes
  (ring/router
   ["/api"  ;; {:middleware [#(wrap-http %)]}
    ["/dummy" kotori/dummy]
    ["/tweet" kotori/tweet]
    ["/tweet-morning" kotori/tweet-morning]
    ["/tweet-evening" kotori/tweet-evening]
    ["/tweet-random" kotori/tweet-random]]))

(defn make-app []
  (ring/ring-handler routes))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (def app (make-app))
  (app {:request-method :post :uri "/api/dummy"})
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

(comment
  (def router
    (r/router
     [["/api/ping" ::ping]
      ["/api/orders/:id" ::order-by-id]]))

  (r/match-by-path router "/api/ping")
  (r/match-by-name router ::ping)

  (r/match-by-path router "/api/orders/1")
  (r/match-by-name router ::order-by-id)

  (r/partial-match? (r/match-by-name router ::order-by-id))
  (r/match-by-name router ::order-by-id {:id 2})
  )

(comment

  (defn handler [_]
    {:status 200, :body "ok"})

  (defn wrap [handler id]
    (fn [request]
      (update (handler request) :wrap (fnil conj '()) id)))

  (def app
    (ring/ring-handler
     (ring/router
      ["/api" {:middleware [[wrap :api]]}
       ["/ping" {:get  handler
                 :name ::ping}]
       ["/admin" {:middleware [[wrap :admin]]}
        ["/users" {:get  handler
                   :post handler}]]])))

  (app {:request-method :get, :uri "/api/admin/users"})
  (app {:request-method :put, :uri "/api/admin/users"})

  (-> app (ring/get-router) (r/match-by-name ::ping))
  )
