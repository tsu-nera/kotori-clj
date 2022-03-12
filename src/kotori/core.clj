(ns kotori.core
  (:gen-class)
  (:require
   ;; [chime.core :as chime]
   ;; [kotori.bot :as bot]
   ;; [taoensso.timbre :as log]
   [integrant.core :as ig]
   [clojure.java.io :as io])
  ;; (:import
  ;;  (java.time Duration Instant))
  )

(def config-file "config.edn")

(defn load-config [config]
  (-> config
      io/resource
      slurp
      ig/read-string
      (doto
          ig/load-namespaces)))

(comment
  (load-config config-file)
  )

;; (def timbre-config {:timestamp-opts {:pattern  "yyyy-MM-dd HH:mm:ss,SSS"
;;                                      :locale   (java.util.Locale. "ja_JP")
;;                                      :tiemzone (java.util.TimeZone/getTimeZone "Asia/Tokyo")}})

;; (init-firebase-app-local!)

;; (defn -main [&args]
;;   (-> config-file
;;       load-config
;;       ig/init))

;; (defn app [& args]
;;   (println "======================================")
;;   (println "Started up Twitter Bot.")
;;   (log/merge-config! timbre-config)
;;   (chime/chime-at (chime/periodic-seq
;;                    (Instant/now)
;;                    (Duration/ofHours 1)
;;                    ;;(Duration/ofMinutes 3)
;;                    )
;;                   (fn [_]
;;                     (bot/tweet-random))))

;;;;;;;;;;;;;;;;;;;
;; Design Journal
;;;;;;;;;;;;;;;;;;;

;; 以下の関数の処理内容を丁寧にみていく.
;; (defn load-config [config]
;;   (-> config
;;       io/resource
;;       slurp
;;       ig/read-string
;;       (doto
;;           ig/load-namespaces)))

;; io/resourceは ファイルパスを返す.
;; (io/resource "config.edn")
;; => #object[java.net.URL 0x2156b557 "file:/home/tsu-nera/repo/kotori-clj/resources/config.edn"]

;; slurpはclojure.coreの関数.
;; filepathをもらうとファイルの中身を文字列として出力.
;; (slurp (io/resource "config.edn"))
;; => ";; for integrant\n{}\n"

;; edn形式のファイルを読む.
;; (ig/read-string ";; for integrant\n{}\n")
;;
;; (ig/read-string (slurp (io/resource "config.edn")))
;; => {:kotori.env/env {}, :kotori.firebase/firebase {:env {:key :kotori.env/env}}}
;;
;; ig/load-namespacesでnamespaceも一緒に読み込む.
;; (doto (ig/read-string "{}") ig/load-namespaces)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (def status (make-status (pick-random)))
;; (def response (private/update-status status))

;; (tweet-random)

;; (def status (guest/get-status "1477034578875277316"))
;; (def status (private/get-status "1477034578875277316"))

;; status
;; => #'kotori.core/load-config;; => #'kotori.core/load-config;; => #'kotori.core/load-config
