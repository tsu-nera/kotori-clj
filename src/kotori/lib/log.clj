(ns kotori.lib.log
  (:require
   [taoensso.timbre :as timbre]))

(defn info
  [args]
  (timbre/info args))

;; FIXME
;; cider-nreplのせいなのかstderrがREPLに表示されないので
;; ラッパー関数で覆ってinfoつかっとく.
(defn error
  [args]
  (timbre/info args))

(comment
  (timbre/refer-timbre)
  ;; (timbre/merge-config! timbre-config)

  (timbre/set-level! :debug)

  (timbre/info "test")
  (timbre/debug "test")
  (timbre/error (Exception. "Doh!") "test")
  timbre/*config*
  )
