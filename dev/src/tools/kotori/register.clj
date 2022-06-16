(ns tools.kotori.register
  (:require
   [devtools :refer [->screen-name ->user-id kotori-by-code]]
   [firebase :refer [db-prod]]
   [firestore-clj.core :as f]
   [kotori.domain.kotori.core :refer [->doc-path]]
   [kotori.lib.firestore :as fs]
   [kotori.lib.json :refer [->json-keyword]]))

(defn kotori->twitter-auth [m]
  (-> m
      (select-keys
       [:auth-token :ct0 :login-ip :creation-ip])
      (clojure.walk/stringify-keys)
      (->json-keyword)
      (clojure.set/rename-keys {"ct_0" "ct0"})))

(defn update-af-id [code db]
  (let [kotori   (kotori-by-code code)
        user-id  (:user-id kotori)
        af-id    (:dmm-af-id kotori)
        doc-path (->doc-path user-id)]
    (doto (f/doc db doc-path)
      (f/assoc! "dmm_af_id" af-id))))
#_(update-af-id "0031" (db-prod))

(defn kotori-edn->fs!
  "kotori.ednの情報をfirestoreへ"
  ([code]
   (kotori-edn->fs! code (db-prod)))
  ([code db]
   (let [kotori       (kotori-by-code code)
         user-id      (:user-id kotori)
         af-id        (:dmm-af-id kotori)
         twitter-auth (kotori->twitter-auth kotori)
         doc-path     (->doc-path user-id)]
     (doto (f/doc db doc-path)
       (f/assoc! "twitter_auth" twitter-auth)
       (f/assoc! "dmm_af_id" af-id)))))
#_(kotori-edn->fs! "0003")
