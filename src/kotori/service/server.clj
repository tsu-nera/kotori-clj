(ns kotori.service.server
  (:gen-class)
  (:require
   [integrant.core :as ig]
   [kotori.procedure.kotori :as kotori]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.response :as response]))

(defn handler [req]
  (let [tweet (kotori/tweet-random)]
    (response/response "OK")))


(defn serve
  ;; [port]
  []
  (run-jetty (-> handler
                 wrap-keyword-params
                 wrap-json-params
                 wrap-json-response
                 wrap-params
                 )
             {:host  "0.0.0.0"
              :port  8888
              :join? false}))


(defmethod ig/init-key ::app [_ _]
  (serve))

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

(comment

  (defn handler [request]
    (println "handler called")
    {:status  200
     :headers {"Content-Type" "text/html"}
     :body    "Hello World"})

  (def server (serve))

  server

  (.stop server)
  (.start server)
  )
