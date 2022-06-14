(ns tools.kotori.register
  (:require
   [devtools :refer [->screen-name ->user-id kotori-by-code]]
   [firebase :refer [db-prod]]
   [firestore-clj.core :as f]
   [kotori.domain.kotori.core :refer [->doc-path]]
   [kotori.lib.firestore :as fs]
   [kotori.lib.json :refer [->json-keyword]]))

(defn edn->twitter-auth->fs!
  "kotori.ednの情報をfirestoreへ"
  [code]
  (let [kotori       (kotori-by-code code)
        user-id      (:user-id kotori)
        twitter-auth (-> kotori
                         (select-keys
                          [:auth-token :ct0 :login-ip :creation-ip])
                         (clojure.walk/stringify-keys)
                         (->json-keyword)
                         (clojure.set/rename-keys {"ct_0" "ct0"}))
        doc-path     (->doc-path user-id)]
    (-> (db-prod)
        (f/doc doc-path)
        (f/assoc! "twitter_auth" twitter-auth))))
#_(edn->twitter-auth->fs! "0040")
