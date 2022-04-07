(ns kotori.core
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [integrant.core :as ig]))

;; integrant configuration map
(def config-file "config.edn")

(defn load-config
  [config]
  (-> config
      io/resource
      slurp
      ig/read-string
      (doto
       (ig/load-namespaces))))

(defonce ^:private system nil)

(def alter-system (partial alter-var-root #'system))

(defn start-system! []
  (alter-system (constantly
                 (-> config-file
                     load-config
                     (ig/init)))))

(defn stop-system! []
  (alter-system ig/halt!))

(defn -main
  [& _args]
  (start-system!))

;;;;;;;;;;;;;;;;;;;
;; Design Journal
;;;;;;;;;;;;;;;;;;;

(comment
  (start-system!)
  (stop-system!)
  )

(comment
  (load-config config-file)
  )
