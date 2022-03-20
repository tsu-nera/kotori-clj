(ns kotori.service.server
  (:gen-class)
  (:require
   [camel-snake-kebab.core :refer [->kebab-case ->snake_case]]
   [camel-snake-kebab.extras :refer [transform-keys]]
   [integrant.core :as ig]
   [kotori.procedure.kotori :as kotori]
   [kotori.procedure.router :refer [make-app]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.response :as resp]))

(defn wrap-db [handler db]
  (fn [request]
    (handler (assoc-in request [:params :db] db))))

;; requestをkebab-case, responseを snake_caseに変換.
(defn wrap-kebab-case-keys [handler]
  (fn [request]
    (let [response (-> request
                       (update :params (partial transform-keys
                                                #(->kebab-case % :separator \_)))
                       handler)]
      (transform-keys #(->snake_case % :separator \-) response))))

(defn serve [opts db]
  (let [app (make-app)]
    (run-jetty
     (-> app
         wrap-kebab-case-keys
         wrap-keyword-params
         wrap-json-params
         wrap-params
         wrap-json-response
         (wrap-db db))
     opts)))

(defmethod ig/init-key ::app [_ {:keys [opts db]}]
  (serve opts db))

;; Firesore InterfaceはAutoClosableというInteraceを実装しているようで
;; 名前からしてFirebaseAppを消せば勝手にFirestoreも消えそうだな.
(defmethod ig/halt-key! ::app [_ server]
  (.stop server))

;; (defn -main
;;   [& _args]
;;   ;; (serve (Long/parseLong (System/getenv "PORT")))
;;   (serve)
;;   )
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment

  (require '[integrant.repl.state :refer [config system]])
  (require '[clojure.pprint :refer [pprint]])

  (def app (make-app))
  (def db (:db (::app config)))
  (def request-dummy {:request-method :post :uri "/api/dummy"})

  (def handler (wrap-http
                (wrap-db
                 (wrap-params
                  (wrap-json-response
                   (wrap-json-params
                    (wrap-keyword-params (make-app)))))
                 db)))

  ((wrap-keyword-params app)
   request-dummy)

  (handler {:request-method :post :uri "/api/dummy"})
  )

(comment

(kotori/pick-random)
(kotori/tweet-random)
)

(def handler-morning (fn [_] ((println "おはよう"))))

(def opts
  {:port    8080
   :handler handler-morning})

(-> opts
    (dissoc :handler)
    (assoc :join? false))

;; => {:port 8080, :join? false}

(comment

  (defn handler [request]
    (println "handler called")
    {:status  200
     :headers {"Content-Type" "text/html"}
     :body    "Hello World"})

  (def server (serve {:host  "0.0.0.0"
                      :port  8888
                      :join? false}))

  server

  (.stop server)
  (.start server)
  )
