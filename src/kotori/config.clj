(ns kotori.config
  (:require [config.core :as config]
            [integrant.core :as ig]
            [clojure.java.io :as io]))

(defmethod ig/init-key ::config [_ values]
  (println "prepare config variables")
  ;;(merge values {:cred-path "resources/private/dev/credentials.json"})
  (config/reload-env)
  )

(defmethod ig/halt-key! ::config [_ _]
  (println "destroy config variables")
  nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (require '[clojure.java.io :as io])

;; (io/file "config.edn")
;; => #object[java.io.File 0x71fda6e1 "config.edn"]
;; (io/resource "config.edn")
;; (io/resource "credentials.json")
;; => #object[java.net.URL 0x7f3b3463 "file:/home/tsu-nera/repo/kotori-clj/resources/config.edn"]
;; (or (io/resource "config.edn") (io/file "config.edn"))
;; => #object[java.net.URL 0x4fee40d2 "file:/home/tsu-nera/repo/kotori-clj/resources/config.edn"]
;;

;; (io/resource "private/dev/config.edn")
;; (io/resource "private/dev/credentials.json")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; environを使って読む環境変数は
;; integrantを使ったとしてもrefreshで更新されないことに注意.
;;
;; ref. https://github.com/weavejester/environ/issues/16
;; environはprocessの環境変数を読むものであるのでそれは不定であり変更を許さない.
;; replから動的に変更したいならuser.cljでよろしくやってくれ,
;; environはその責務は果たさない.

;; environをwrapしたconfigを利用することで,
;; プロセス環境変数とEDN構成ファイルの2つの抽象のモジュールを作成する.
;; https://github.com/yogthos/config
;;
;; なぜこの抽象がほしいかというと,
;; Google Cloud Runでは環境変数でないとcredの扱いに困る.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (start) で実行される部分
;; (defmethod ig/init-key ::env [_ _]
;;   (println "loading environment via environ")
;;   (let [running   (env :env)
;;         log-level (decode-log-level (env :log-level))]
;;     (println "running in " running)
;;     (println "log-level " log-level)
;;     (when (.contains ["test" "dev"] running)
;;       (println "orchestra instrument is active")
;;       (st/instrument))
;;     {:running   running
;;      :log-level log-level}))
