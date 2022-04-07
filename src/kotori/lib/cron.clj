(ns kotori.lib.cron
  (:require
   [chime.core :as chime])
  (:import
   (java.time
    Instant)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; (def app (atom :initialized))

;; (defn start! []
;;   (println "running kotori start.")
;;   (chime/chime-at (chime/periodic-seq
;;                    (chime/now)
;;                    (Duration/ofMinutes 1))
;;                   #((kotori/tweet-random) %)
;;                   {:on-finished (fn []
;;                                   (println "running kotori end."))}))

;; (defn stop! [app]
;;   (.close app))
;; (defn -main
;;   [& _args]
;;   (start!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  ;; chime libの研究
  ;; https://github.com/jarohen/chime/blob/master/src/chime/core.clj
  chime/*clock*
  ;; 現在時刻取得(java.instant)
  (chime/now)
  ;; => #object[java.time.Instant 0x5b5dfa25 "2022-03-13T23:14:10.425242Z"]
  (Instant/now)
  ;; => #object[java.time.Instant 0x66f3c765 "2022-03-13T23:14:12.569066Z"]

  ;;  periodic-seqは無限の遅延シーケンスを作成している.
  ;;  開始と反復間隔の２つを引数にもらう.
  ;;  iterateは関数を引数に漸化式を構築する.
  (defn periodic-seq [^Instant start duration-or-period]
    (iterate #(.addTo duration-or-period ^Instant %) start))
  ;;  without-past-timesをつかうとnowを省略.

  ;; https://github.com/jarohen/chime/blob/fa0b6e8c0f68e6d6134aee6ba9eb2ce032ba65b0/src/chime/core.clj#L97
  ;; schedule-loopという関数を作成して再帰呼び出しにより無限ループを作成.
  ;; それをシングルスレッドプール上のスレッドで動作させている.
  ;; すなわちchimeのプロセスの実体はThreadPool上のスレッドで動作する再帰関数.
  )
