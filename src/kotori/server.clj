(ns kotori.server
  (:gen-class)
  (:require
   [kotori.firebase :refer [init-firebase-app-prod!]]
   [kotori.bot :as bot]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.response :as response]))

(defn handler [req]
  ;; (prn req)
  (init-firebase-app-prod!)
  (let [;;params (:params req)
        ;;status (:status params)
        ;;tweet (private/update-status status)
        tweet (bot/tweet-random)]
    (response/response "OK")))

(defn serve [port]
  (run-jetty
   (-> handler
       wrap-keyword-params
       wrap-json-params
       wrap-json-response
       wrap-params
       )
   {:host  "0.0.0.0"
    :port  port
    ;; run-jetty takes over the thread by default, which is bad in a REPL
    :join? false}))

(defn -main
  [& _args]
  (serve (Long/parseLong (System/getenv "PORT"))))
