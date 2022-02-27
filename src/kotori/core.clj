(ns kotori.core
  (:gen-class)
  (:require
   [environ.core :refer [env]]
   [chime.core :as chime]
   [kotori.runner :as runner]
   [taoensso.timbre :as log]
   [integrant.core :as ig]
   [clojure.java.io :as io])
  (:import
   (java.time Duration Instant)))

(def config-file
  (if-let [config-file (env :config-file)]
    config-file
    "config.edn"))

(defn load-config [config]
  (-> config
      io/resource
      slurp
      ig/read-string
      (doto
          ig/load-namespaces)))

(def timbre-config {:timestamp-opts {:pattern  "yyyy-MM-dd HH:mm:ss,SSS"
                                     :locale   (java.util.Locale. "ja_JP")
                                     :tiemzone (java.util.TimeZone/getTimeZone "Asia/Tokyo")}})

;; (init-firebase-app-local!)

(defn -main [&args]
  (-> config-file
      load-config
      ig/init))

(defn app [& args]
  (println "======================================")
  (println "Started up Twitter Bot.")
  (log/merge-config! timbre-config)
  (chime/chime-at (chime/periodic-seq
                   (Instant/now)
                   (Duration/ofHours 1)
                   ;;(Duration/ofMinutes 3)
                   )
                  (fn [_]
                    (runner/tweet-random))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (def status (make-status (pick-random)))
;; (def response (private/update-status status))

;; (tweet-random)

;; (def status (guest/get-status "1477034578875277316"))
;; (def status (private/get-status "1477034578875277316"))

;; status
