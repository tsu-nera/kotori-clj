(ns kotori.procedure.router
  (:require
   [kotori.procedure.kotori :as kotori]
   [reitit.core :as r]
   [reitit.ring :as ring]))

(def resp-ok {:status 200, :body "ok"})

(defn wrap [handler]
  (fn [request]
    (handler)
    resp-ok))

(def router
  (ring/router
   ["/api" {:middleware [#(wrap %)]}
    ["/tweet-morning" kotori/tweet-morning]
    ["/tweet-evening" kotori/tweet-evening]
    ["/tweet-random" kotori/tweet-random]]))

(def app (ring/ring-handler router))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(app {:request-method :post :uri "/api/tweet-evening"})

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
