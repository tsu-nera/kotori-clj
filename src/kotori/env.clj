(ns kotori.env
  (:require [environ.core :refer [env]]
            [integrant.core :as ig]))

(defmethod ig/init-key ::env [_ values]
  (println "init environment variables")
  (merge values {:cred-path "resources/private/dmm-fanza-dev-firebase-adminsdk.json"}))

(defmethod ig/halt-key! ::env [_ _]
  (println "destroy environment variables")
  nil)

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
