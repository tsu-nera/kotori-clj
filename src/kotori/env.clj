(ns kotori.env
  (:require [environ.core :refer [env]]
            [integrant.core :as ig]))

(defmethod ig/init-key ::env [_ _]
  (println "init environment variables")
  (let [environment (env :env)]
    (println "runnning in " environment)
    {
     :env environment
     }))

(defmethod ig/halt-key! ::env [_ _]
  (println "destroy environment variables")
  nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
