(ns kotori.service.bot
  (:require
   [chime.core :as chime]
   [integrant.core :as ig]
   [kotori.procedure.kotori :as kotori]
   [taoensso.timbre :as log])
  (:import
   (java.time
    Duration
    Instant)))

;; TODO あとでなんとかする. とりあえずの対応.
;; integrant保留中のためnamespaceに状態をbindingする.
(def app (atom :initialized))

(defn start! []
  (println "Running twitter bot.")
  (chime/chime-at (chime/periodic-seq
                   (chime/now)
                   (Duration/ofMinutes 1))
                  #((kotori/tweet-random) %)
                  {:on-finished (fn []
                                  (println "Running twitter bot end."))}))

;; chime/chime-atで生成される無名関数は
;; JavaのAutoClosableをInterfaceを実装しているため,
;; .closeを呼び出すことによって終了する仕組みのようだ.
(defn stop! [app]
  (.close app))

;; (defmethod ig/init-key ::app [_ _]
;;   (start!))

;; (defmethod ig/halt-key! ::app [_ app]

(defn -main
  [& _args]
  (start!))

(comment
  chime/*clock*
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; chime libの研究
  ;; https://github.com/jarohen/chime/blob/master/src/chime/core.clj

  ;; 現在時刻取得(java.instant)
  (chime/now)
  ;; => #object[java.time.Instant 0x5b5dfa25 "2022-03-13T23:14:10.425242Z"]
  (Instant/now)
  ;; => #object[java.time.Instant 0x66f3c765 "2022-03-13T23:14:12.569066Z"]

  ;;  periodic-seqは無限の遅延シーケンスを作成している.
  ;;  開始と反復間隔の２つを引数にもらう.
  ;;  iterateは関数を引数に漸化式を構築する.
  ;; (defn periodic-seq [^Instant start ^TemporalAmount duration-or-period]
  ;;    (iterate #(.addTo duration-or-period ^Instant %) start))
  ;;  without-past-timesをつかうとnowを省略.

  ;; https://github.com/jarohen/chime/blob/fa0b6e8c0f68e6d6134aee6ba9eb2ce032ba65b0/src/chime/core.clj#L97
  ;; schedule-loopという関数を作成して再帰呼び出しにより無限ループを作成.
  ;; それをシングルスレッドプール上のスレッドで動作させている.
  ;; すなわちchimeのプロセスの実体はThreadPool上のスレッドで動作する再帰関数.

  ;; これは無限遅延シーケンス?
  ;; (def tmp-app
  ;; (chime/periodic-seq
  ;;  (chime/now)
  ;;  (Duration/ofMinutes 1))
  ;;    )

  (def tmp-app (atom nil))
  (reset! tmp-app (app))

  ;; 無名関数(reify)
  @tmp-app

  ;; コレで止まる.
  (.close @tmp-app)

  ;; redefで処理の終了待ち合わせ.
  @tmp-app

  )
