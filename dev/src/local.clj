(ns local
  (:refer-clojure :exclude [proxy])
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :refer [pprint]]
   [clojure.repl :refer :all]
   [clojure.tools.namespace.repl :refer [refresh]]
   [devtools :refer :all :as t]
   [firebase :refer [creds-dev creds-prod]]
   [firestore :refer [open-product open-amateur open-vr open-anime]]
   [hashp.core]
   [integrant.repl :refer
    [clear halt go init prep set-prep! reset reset-all suspend resume]]
   [integrant.repl.state :refer [config system]]
   [kotori.core :as core]
   [kotori.lib.net :refer [get-global-ip]]
   [kotori.service.firebase :refer [get-app get-db delete-app!]]
   [tools.dmm :refer [open-dmm get-dmm prepare-videoa!]]
   [tools.twitter :refer [delete-tweet!]]))

(def env-dev "private/dev/env.edn")
(def env-prod "private/prod/env.edn")

(defn- init-system!
  [creds env]
  (-> core/config-file
      core/load-config
      (assoc-in [:kotori.service.env/env :path] env)
      (assoc-in [:kotori.service.firebase/app :path] creds)
      (assoc :firebase/app {})
      (constantly)
      (set-prep!))
  (prep)
  (init)
  :initialized)

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
