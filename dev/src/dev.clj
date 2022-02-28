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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (start)

;; clojure.core constantly
;; 定数から関数を作成する. 引数が高階関数の関数を呼び出すときにつかう.
;; (constantly (kotori-core/load-config "config.edn"))
;;
;; set-prep!でintegrantの設定を定義.
;; (igr/set-prep! (constantly (kotori-core/load-config config-file)))
;;
;; prepでset-prep!で宣言した宣言を環境に用意.
;; 設定はintegrant.rep.state/configにbindされる.
;;
;; initで環境に適用, reset再適用.
;; integrantが確か環境をインスタンスで管理するので instanceの状態にsetするイメージかな？
