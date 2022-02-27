(ns dev
  (:require
   [kotori.core :as kotori-core]
   [integrant.repl :as igr]))

(defn start
  ([]
   (start kotori-core/config-file))
  ([config-file]
   (igr/set-prep! (constantly (kotori-core/load-config config-file)))
   (igr/prep)
   (igr/init)))

(defn stop []
  (igr/halt))

(defn restart []
  (igr/reset-all))
