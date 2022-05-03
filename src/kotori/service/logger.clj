(ns kotori.service.logger
  (:require
   [integrant.core :as ig]
   [kotori.lib.time :as time]
   [taoensso.timbre :as timbre]))

(def timbre-config
  {:timestamp-opts {:pattern  time/format-log
                    :locale   time/locale-jst
                    :timezone time/tz-jst}})

(defn reset-config []
  (timbre/set-config! {})
  (timbre/set-config! timbre/default-config))

(defmethod ig/init-key ::logger [_ _]
  (reset-config)
  (timbre/merge-config! timbre-config))
