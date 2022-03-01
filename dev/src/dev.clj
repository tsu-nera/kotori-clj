(ns dev
  (:require
   [clojure.tools.namespace.repl :refer [refresh]]
   [integrant.repl :refer [clear halt go init prep set-prep! reset reset-all]]
   [integrant.repl.state :refer [config system]]
   [kotori.core :as kotori-core]
   ))

(defn start
  ([]
   (start kotori-core/config-file))
  ([config-file]
   (-> config-file
       (kotori-core/load-config)
       (assoc :kotori.config/config {:development? true :local? true})
       (constantly)
       (set-prep!))
   (prep)
   (init)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; (def config-map (kotori-core/load-config "config.edn"))
;; (def dev-map {:local? true :dev? true})
;;
;; (merge config-map dev-map)

;; config-edn

;; (start)
;; (set-prep! (constantly  {:development? true
;;                          :local?       true}))
;; (prep)
;;
;; => これで intengrant.repl.state.configにmapが設定される.

;; (set-prep! (constantly (kotori-core/load-config "config.edn")))
;; config

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
