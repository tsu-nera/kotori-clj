(ns kotori.core
  (:gen-class)
  (:require
   [integrant.core :as ig]
   [taoensso.timbre :as log]
   [clojure.java.io :as io])
  )

(def config-file "config.edn")

(defn load-config [config]
  (-> config
      io/resource
      slurp
      ig/read-string
      (doto
          ig/load-namespaces)))

(defn -main
  [& _args]
  (let [config (load-config config-file)]
    (ig/init config)))


;;;;;;;;;;;;;;;;;;;
;; Design Journal
;;;;;;;;;;;;;;;;;;;

(comment
  (def timbre-config {:timestamp-opts {:pattern  "yyyy-MM-dd HH:mm:ss,SSS"
                                       :locale   (java.util.Locale. "ja_JP")
                                       :timezone (java.util.TimeZone/getTimeZone "Asia/Tokyo")}})

  (log/merge-config! timbre-config)
  (log/info "test")

  log/*config*
  )

(comment
  (load-config config-file)
  )

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

