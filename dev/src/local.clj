(ns local
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.repl :refer :all]
   [clojure.tools.namespace.repl :refer [refresh]]
   [hashp.core]
   [integrant.repl :refer
    [clear halt go init prep set-prep! reset reset-all suspend resume]]
   [integrant.repl.state :refer [config system]]
   [kotori.core :as core]
   [kotori.service.bot :as bot]
   [kotori.service.firebase :refer [get-app get-db delete-app!]]))

(def creds-dev "private/dev/credentials.json")
(def env-dev "private/dev/env.edn")
(def creds-prod "private/prod/credentials.json")
(def env-prod "private/prod/env.edn")

(defn- init-system!
  [creds env]
  (-> core/config-file
      core/load-config
      (assoc-in [:kotori.service.env/creds :path] creds)
      (assoc-in [:kotori.service.env/env :path] env)
      ;; (assoc-in [:kotori.service.firebase/app :config] creds)
      ;; (assoc-in [:kotori.model.kotori/db :config] config)
      ;; (assoc-in [:kotori.model.tweet/db :config] config)
      (constantly)
      (set-prep!))
  (prep)
  (init)
  :initialized)

(defn env []
  (get system :kotori.service.env/env))

(defn dev? []
  (= (:env (env)) :development))

(defn prod? []
  (= (:env (env)) :production))

(defn dev []
  (init-system! creds-dev env-dev)
  :development)

(defn prod []
  (init-system! creds-prod env-prod)
  :production)

;; integrantのconfig.ednをいじったら clearで
;; 古いconfigの設定を破棄する必要がある.
(defn stop []
  (clear)
  (halt)
  :stopped)

(defn run-kotori []
  (bot/start!)
  :running)

(defn stop-kotori []
  (bot/stop!)
  :stopped)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (def db (get-db))
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
