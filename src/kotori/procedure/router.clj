(ns kotori.procedure.router
  (:require
   [kotori.procedure.kotori :as kotori]
   [reitit.core :as r]
   [reitit.ring :as ring]))

(def resp-ok {:status 200, :body "ok"})

(defn tweet-morning-handler [_]
  (kotori/tweet-morning)
  resp-ok)

(defn tweet-evening-handler [_]
  (kotori/tweet-evening)
  resp-ok)

(defn tweet-random-handler [_]
  (kotori/tweet-random)
  resp-ok)

(def router
  (ring/router
   [["/api"
     ["/tweet-morning" {:post tweet-morning-handler}]
     ["/tweet-evening" {:post tweet-evening-handler}]
     ["/tweet-random" {:post tweet-random-handler}]]]))

(def app (ring/ring-handler router))

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
