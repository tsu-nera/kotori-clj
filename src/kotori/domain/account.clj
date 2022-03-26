;; (ns kotori.model.account
;;   (:require
;;    [integrant.core :as ig]
;;    [clojure.set :refer [rename-keys]]
;; [clojure.walk :refer [keywordize-keys]]
;;    [kotori.lib.twitter.guest :as guest]))

;; (def twitter-account (atom nil))

;; (defrecord TwitterAccount
;;     [screnname
;;      userid
;;      auth-token
;;      ct0])

;; (defn doc->twitter-auth
;;   [doc]
;;   (-> doc
;;       (:twitter-auth)
;;       (as-> x (into {} x))
;;       (keywordize-keys)
;;       (rename-keys {:auth_token :auth-token})))

;; (defn set-account!
;;   ([screenname]
;;    (let [userid (guest/resolve-user-id screenname)]
;;      (set-account! screenname userid)))
;;   ([screenname userid]
;;    (let [account (->TwitterAccount screenname userid nil nil)]
;;      (reset! twitter-account account)))
;;   ([screenname userid auth-token ct0]
;;    (let [account (->TwitterAccount screenname userid auth-token ct0)]
;;      (reset! twitter-account account))))


;; (defmethod ig/init-key :kotori.twitter/account [_ {:keys [kotori]}]
;;   (prn kotori)
;;   ;; (let [screenname (:screenname kotori)]
;;   ;;   (doc->twitter-auth kotori)
;;   ;;   )
;;   :initalized)


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ;; kebab-caseだと長い変数名にするとかっこ悪い気がするが,
;; ;; だからこそ積極的にrecordというデータ構造を使ったほうがいいのかも.
;; ;; だいたい単語なんて2つの複合語で収まることが多く,
;; ;; それ以上になるなるぱより大きな抽象でグループにまとめられることが多い気がする.


;; (comment
;;   (require '[clojure.edn :as edn])
;;   (require '[clojure.java.io :as io])

;;   (def twitter-auth (atom {}))

;;   (def config-dev "resources/private/dev/config.edn")
;;   (defn- load-config [config]
;;     (-> config
;;         io/file
;;         slurp
;;         edn/read-string))

;;   (let [{:keys [screenname]} (load-config config-dev)]
;;     (set-account! screenname))
;;   (let [{:keys [screenname userid]} (load-config config-dev)]
;;     (set-account! screenname userid))
;;   (let [{:keys [screenname userid auth-token ct0]} (load-config config-dev)]
;;     (set-account! screenname userid auth-token ct0))
;;   )
