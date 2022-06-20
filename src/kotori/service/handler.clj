(ns kotori.service.handler
  (:require
   [integrant.core :as ig]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.procedure.dmm.amateur :as dmm-amateur]
   [kotori.procedure.dmm.anime :as dmm-anime]
   [kotori.procedure.dmm.doujin :as dmm-doujin]
   [kotori.procedure.dmm.product :as dmm]
   [kotori.procedure.dmm.vr :as dmm-vr]
   [kotori.procedure.kotori.core :as kotori]
   [kotori.procedure.kotori.doujin :as doujin]
   [kotori.procedure.kotori.qvt :as qvt]
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
          info        (kotori/config->info config)]
      (handler (assoc req :info info)))))

(defn wrap-dmm [handler]
  (fn [req]
    (let [env   (:env req)
          creds (api/env->creds env)]
      (handler (assoc req :creds creds)))))

(defn make-app [config-map]
  (ring/ring-handler
   (ring/router
    ["/api" {:middleware [#(wrap-http %)]}
     ["/dmm" {:middleware [#(wrap-dmm %)]}
      ["/crawl-product" {:post dmm/crawl-product!}]
      ["/crawl-products" {:post dmm/crawl-products!}]
      ["/crawl-vr-products" {:post dmm-vr/crawl-products!}]
      ["/crawl-anime-products" {:post dmm-anime/crawl-products!}]
      ["/crawl-amateur-products" {:post dmm-amateur/crawl-products!}]
      ["/crawl-doujin-products" {:post dmm-doujin/crawl-products!}]
      ["/crawl-doujin-voices" {:post dmm-doujin/crawl-voice-products!}]
      ["/crawl-doujin-girls" {:post dmm-doujin/crawl-girls-products!}]
      ["/crawl-qvt-descs" {:post dmm/crawl-qvt-descs!}]]
     ["/kotori" {:middleware [#(wrap-kotori config-map %) #(wrap-dmm %)]}
      ["/dummy" kotori/dummy]
      ["/tweet" kotori/tweet]
      ["/tweet-quoted-video"
       {:post qvt/tweet-quoted-video}]
      ["/tweet-morning" kotori/tweet-morning]
      ["/tweet-evening" kotori/tweet-evening]
      ["/tweet-random" kotori/tweet-random]
      ["/tweet-boys-doujin-image" doujin/tweet-boys-image]
      ["/tweet-girls-doujin-image" doujin/tweet-girls-image]
      ["/tweet-doujin-image" doujin/tweet-boys-image] ;; TODO 後で削除
      ["/tweet-doujin-voice" doujin/tweet-voice]
      ["/get-product" {:get kotori/get-product}]
      ["/select-next-product" {:get kotori/select-next-product}]
      ["/select-next-amateur-videoc"
       {:get kotori/select-next-amateur-videoc}]
      ["/select-next-vr" {:get kotori/select-next-vr}]
      ["/select-next-anime" {:get kotori/select-next-anime}]]])))

(defmethod ig/init-key ::app [_ {:keys [config-map]}]
  (make-app config-map))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(comment
  (require '[devtools :refer [kotori-names ->screen-name kotori-info]]
           '[tools.dmm :refer [creds]]
           '[firebase :refer [db db-prod]])
  (def app (make-app (kotori-names)))

  (def screen-name (->screen-name "0031"))
  (def params {:db          (db-prod)
               :creds       (creds)
               :info        (kotori-info "0031")
               :screen-name screen-name})

  (app {:request-method :post :uri "/api/ping"})
  (app {:request-method :post :uri "/api/dmm/get-product"})
  (app {:request-method :post
        :uri            "/api/dmm/crawl-products"
        :params         {:db    (db-prod)
                         :limit 100
                         :creds (creds)}})

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
        :uri            "/api/kotori/tweet-doujin-voice"
        :params         params})

  )
