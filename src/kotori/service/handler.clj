(ns kotori.service.handler
  (:require
   [integrant.core :as ig]
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

(defn wrap-kotori [bot-configs handler]
  (fn [req]
    (let [screen-name (:screen-name req)
          bot         (get bot-configs screen-name)
          info        (kotori/make-info bot)]
      (handler (assoc req :info info)))))

(defn make-app [bot-configs]
  (ring/ring-handler
   (ring/router
    ["/api" {:middleware [#(wrap-http %)]}
     ["/ping" {:post ping/ping-pong}]
     ["/dmm"
      ["/crawl-product" {:post dmm/crawl-product!}]
      ["/crawl-products" {:post dmm/crawl-products!}]
      ["/select-next-product" {:get strategy/select-next-product}]]
     ["/kotori" {:middleware [#(wrap-kotori bot-configs %)]}
      ["/dummy" kotori/dummy]
      ["/tweet" kotori/tweet]
      ["/tweet-quoted-video"
       {:post kotori/tweet-quoted-video}]
      ["/tweet-morning" kotori/tweet-morning]
      ["/tweet-evening" kotori/tweet-evening]
      ["/tweet-random" kotori/tweet-random]]])))

(defmethod ig/init-key ::app [_ {:keys [bot]}]
  (make-app bot))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(comment
  (require '[devtools :refer [kotori-names ->screen-name]]
           '[firebase :refer [db]])
  (def app (make-app (kotori-names)))
  (def screen-name (->screen-name "0003"))

  (app {:request-method :post :uri "/api/ping"})
  (app {:request-method :post :uri "/api/dmm/get-product"})

  (app {:request-method :post
        :uri            "/api/kotori/dummy"
        :params         {:text        "テスト投稿"
                         :screen-name ""}})

  (app {:request-method :post
        :uri            "/api/kotori/tweet-random"})

  (app {:request-method :post
        :params         {:db          (db)
                         :screen-name screen-name}
        :uri            "/api/kotori/tweet-quoted-video"})
  )
