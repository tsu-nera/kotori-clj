(ns local
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.repl :refer :all]
   [clojure.tools.namespace.repl :refer [refresh]]
   [hashp.core]
   [integrant.repl :refer [clear halt go init prep set-prep! reset reset-all suspend resume]]
   [integrant.repl.state :refer [config system]]
   [kotori.core :as kotori-core]
   [kotori.procedure.kotori :refer [tweet]]
   [kotori.service.bot :as bot]))

(def env-dev
  {:env       :development
   :cred-path "resources/private/dev/credentials.json"})

(def env-prod
  {:env       :production
   :cred-path "resources/private/prod/credentials.json"})

(def config-dev "resources/private/dev/config.edn")
(def config-prod "resources/private/prod/config.edn")

(defn- load-config [config]
  (-> config
      io/file
      slurp
      edn/read-string))

(defn- init-system!
  [env config]
  (-> kotori-core/ig-config
      (assoc-in [:kotori.service.firebase/app :config] env)
      (assoc-in [:kotori.model.kotori/db :config] config)
      (assoc-in [:kotori.model.tweet/db :config] config)
      (constantly)
      (set-prep!))
  (prep)
  (init)
  :initialized)

(defn env []
  (merge (get-in config [:kotori.service.firebase/app :config])
         (get-in config [:kotori.model.kotori/db :config])))

(defn dev []
  (let [config (load-config config-dev)]
    (init-system! env-dev config)
    :development))

(defn prod []
  (let [config (load-config config-prod)]
    (init-system! env-prod config)
    :production))

(defn restart []
  (clear)
  (reset-all))

(defn run-kotori []
  (bot/start!)
  :running)

(defn stop-kotori []
  (bot/stop!)
  :stopped)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;; FirebaseApp instanceの削除.
  (require '[kotori.service.firebase :refer [delete-app!]])
  (delete-app!)
  )

(comment
  (require '[build :as b])
  (b/clean nil)
  (b/jar nil)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (kotori-core/load-config kotori-core/config-file)

  (def config-dev "resources/private/dev/config.edn")
  (require '[clojure.java.io :as io])
  (io/resource config-dev)

  (require '[clojure.edn :as edn])

  (defn load-config [config]
    (-> config
        io/file
        slurp
        edn/read-string))

  (load-config config-dev)
  )

;; (def config-map (kotori-core/load-config "config.edn"))
;; (def dev-map {:local? true :dev? true})
;;
;; (merge config-map dev-map)

;; config-edn

;; (start);; (set-prep! (constantly  {:development? true
;;                          :local?       true}))
;; (prep)
;;
;; => これで intengrant.repl.state.configにmapが設定される.

;; (set-prep! (constantly (kotori-core/load-config "config.edn")))

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
