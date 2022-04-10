(ns kotori.service.server
  (:gen-class)
  (:require
   [integrant.core :as ig]
   [kotori.lib.json :as json]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]))

(defn wrap-db
  [handler db]
  (fn [request]
    (handler (assoc-in request [:params :db] db))))

(defn wrap-env
  [handler env]
  (fn [request]
    (handler (assoc-in request [:params :env] env))))

(defn wrap-kebab-case-keys
  "Request Map をkebab-case, Response Mapを snake_caseに変換."
  [handler]
  (fn [request]
    (let [response
          (-> request
              (update :params (partial json/->clj))
              handler)]
      (json/->json-keyword response))))

(defn serve [{:keys [db config env handler]}]
  (run-jetty
   (-> handler
       wrap-kebab-case-keys
       wrap-keyword-params
       wrap-json-params
       wrap-params
       wrap-json-response
       (wrap-env env)
       (wrap-db db))
   config))

(defmethod ig/init-key ::server [_ m]
  (serve m))

;; Firesore InterfaceはAutoClosableというInteraceを実装しているようで
;; 名前からしてFirebaseAppを消せば勝手にFirestoreも消えそうだな.
(defmethod ig/halt-key! ::server [_ server]
  (.stop server))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defn -main
;;   [& _args]
;;   ;; (serve (Long/parseLong (System/getenv "PORT")))
;;   (serve)
;;   )
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
