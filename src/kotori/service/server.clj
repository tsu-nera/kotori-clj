(ns kotori.service.server
  (:gen-class)
  (:require
   [integrant.core :as ig]
   [kotori.procedure.kotori :as kotori]
   [kotori.procedure.router :refer [make-app]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.response :as resp]))

(defn wrap-http [handler]
  (fn [request]
    (-> request
        :params
        (handler)
        (resp/response))))

(defn wrap-db [handler db]
  (fn [request]
    (handler (assoc-in request [:params :db] db))))

(defn serve [opts db]
  (let [app (make-app)]
    (run-jetty #p (-> app
                      wrap-keyword-params
                      wrap-json-params
                      wrap-json-response
                      wrap-params
                      (wrap-db db)
                      wrap-http)
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
