(ns kotori.service.server
  (:gen-class)
  (:require
   [camel-snake-kebab.core :refer [->kebab-case ->snake_case]]
   [camel-snake-kebab.extras :refer [transform-keys]]
   [integrant.core :as ig]
   [kotori.lib.api.handler :refer [make-endpoint]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]))

(defn wrap-db
  [handler db]
  (fn [request]
    (handler (assoc-in request [:params :db] db))))

(defn wrap-kebab-case-keys
  "Request Map をkebab-case, Response Mapを snake_caseに変換."
  [handler]
  (fn [request]
    (let [response
          (-> request
              (update :params (partial transform-keys
                                       #(->kebab-case % :separator \_)))
              handler)]
      (transform-keys #(->snake_case % :separator \-) response))))

(def endpoint (make-endpoint))

(defn serve [{:keys [db config]}]
  (run-jetty
   (-> #'endpoint
       wrap-kebab-case-keys
       wrap-keyword-params
       wrap-json-params
       wrap-params
       wrap-json-response
       (wrap-db db))
   config))

(defmethod ig/init-key ::server [_ m]
  (serve m))

;; Firesore InterfaceはAutoClosableというInteraceを実装しているようで
;; 名前からしてFirebaseAppを消せば勝手にFirestoreも消えそうだな.
(defmethod ig/halt-key! ::server [_ server]
  (.stop server))

;; (defn -main
;;   [& _args]
;;   ;; (serve (Long/parseLong (System/getenv "PORT")))
;;   (serve)
;;   )
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

  #_server

  (.stop server)
  (.start server)
  )
