(ns kotori.lib.log
  (:require
   [kotori.lib.time :as time]
   [taoensso.timbre :as log])
  (:import
   (java.time
    Instant)))

(def timbre-config
  {:timestamp-opts {:pattern  time/format-log
                    :locale   time/locale-jst
                    :timezone time/tz-jst}})

(comment
  (log/merge-config! timbre-config)
  (log/info "test")

  log/*config*
  )

