(ns kotori.core
  (:gen-class)
  (:require
   [clojure.edn :as edn]
   [integrant.core :as ig]
   [taoensso.timbre :as log]
   [clojure.java.io :as io])
  )

;; integrant configuration map
(def ig-config-file "config.edn")

(defn load-ig-config [config]
  (-> config
      io/resource
      slurp
      ig/read-string
      (doto
          (ig/load-namespaces))))

(defn- load-edn [config]
  (-> config
      io/file
      slurp
      edn/read-string))

(def ig-config (load-ig-config ig-config-file))

(def env-prod {:env       :production
               :cred-path "resources/private/prod/credentials.json"})
(def config-prod (load-edn "resources/private/prod/config.edn"))

(defonce ^:private system nil)

(def alter-system (partial alter-var-root #'system))

(defn system-start []
  (alter-system (constantly
                 (-> ig-config
                     (assoc-in [:kotori.service.firebase/app :config] env-prod)
                     (assoc-in [:kotori.model.kotori/db :config] config-prod)
                     (assoc-in [:kotori.model.tweet/db :config] config-prod)
                     (ig/init)))))

(defn system-stop []
  (alter-system ig/halt!))

(defn -main
  [& _args]
  (system-start))

;;;;;;;;;;;;;;;;;;;
;; Design Journal
;;;;;;;;;;;;;;;;;;;


(comment
  system
  (system-start)
  (system-stop)

  (ig/dependency-graph ig-config)
  )

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
