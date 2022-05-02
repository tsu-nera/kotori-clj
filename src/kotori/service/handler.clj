(ns kotori.service.handler
  (:require
   [integrant.core :as ig]
   [kotori.procedure.dmm.product :as dmm]
   [kotori.procedure.kotori.core :as kotori]
   [kotori.procedure.kotori.qvt :as qvt]
   [kotori.procedure.ping :as ping]
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

(defn wrap-kotori [config-map handler]
  (fn [req]
    (let [screen-name (:screen-name req)
          config      (get config-map screen-name)
          info        (kotori/make-info config)]
      (handler (assoc req :info info)))))

(defn make-app [config-map]
  (ring/ring-handler
   (ring/router
    ["/api" {:middleware [#(wrap-http %)]}
     ["/ping" {:post ping/ping-pong}]
     ["/dmm"
      ["/crawl-product" {:post dmm/crawl-product!}]
      ["/crawl-products" {:post dmm/crawl-products!}]
      ["/crawl-qvt-descs" {:post dmm/crawl-qvt-descs!}]]
     ["/kotori" {:middleware [#(wrap-kotori config-map %)]}
      ["/dummy" kotori/dummy]
      ["/tweet" kotori/tweet]
      ["/tweet-quoted-video"
       {:post qvt/tweet-quoted-video}]
      ["/tweet-morning" kotori/tweet-morning]
      ["/tweet-evening" kotori/tweet-evening]
      ["/tweet-random" kotori/tweet-random]
      ["/select-next-product"
       {:get kotori/select-next-product}]
      ["/select-next-amateur"
       {:get kotori/select-next-amateur}]]])))

(defmethod ig/init-key ::app [_ {:keys [config-map]}]
  (make-app config-map))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(comment
  (require '[devtools :refer [kotori-names ->screen-name]]
           '[firebase :refer [db]])
  (def app (make-app (kotori-names)))

  (def screen-name (->screen-name "0003"))
  (def params {:db          (db)
               :screen-name screen-name})

  (app {:request-method :post :uri "/api/ping"})
  (app {:request-method :post :uri "/api/dmm/get-product"})

  (app {:request-method :get
        :uri            "/api/kotori/select-next-product"
        :params         {:db (db) :screen-name screen-name}})

  (app {:request-method :post
        :uri            "/api/kotori/dummy"
        :params         {:text        "テスト投稿"
                         :screen-name screen-name}})

  (app {:request-method :post
        :uri            "/api/kotori/tweet-random"
        :params         params})

  (app {:request-method :post
        :uri            "/api/kotori/tweet-quoted-video"
        :params         params})

  )
