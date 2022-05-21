(ns kotori.domain.dmm.genre.videoa
  "refs.https://www.dmm.co.jp/digital/videoa/-/genre/"
  (:require
   [kotori.domain.dmm.core :refer [doc-path]]
   [kotori.domain.dmm.genre.interface :as if]
   [kotori.lib.io :as io]))

(def genre-path "dmm/genre/videoa.edn")
(defonce genres (->> genre-path io/load-edn))

(defonce id-name-map (-> genres if/->id-name-map))
(defonce name-id-map (-> genres if/->name-id-map))

(def names->genre-ids
  (partial if/names->genre-ids name-id-map))

(def amateur-genre-id (get name-id-map "素人"))
(def amateur-ids
  (-> ["素人" "ナンパ" "ハメ撮り"]
      names->genre-ids))

(def coll-path (str doc-path "/products"))
(defn ->doc-path [cid] (str coll-path "/" cid))

(defrecord Videoa [floor]
           if/Genre
           (id->name [_ id] (get id-name-map id))
           (name->id [_ name] (get name-id-map name))
           (->coll-path [_] coll-path)
           (->doc-path [_ cid] (->doc-path cid)))

;; 2022.05現在, VR専用はハイクオリティVRを内包する
(def vr-only-id (get name-id-map "VR専用"))

(def vr-ids
  (-> ["VR専用" "ハイクオリティVR"]
      names->genre-ids))

(def antisocial-ids
  (-> ["盗撮・のぞき" "ドラッグ" "監禁"]
      names->genre-ids))

(def violent-ids
  (-> ["残虐表現" "鬼畜" "拷問" "蝋燭" "鼻フック"]
      names->genre-ids))

(def dirty-ids
  (-> ["スカトロ" "食糞" "放尿・お漏らし"
       "飲尿" "脱糞" "浣腸" "ゲロ" "異物挿入"]
      names->genre-ids))

(def trans-ids
  (-> ["女装・男の娘" "ニューハーフ" "ふたなり"
       "ゲイ" "性転換・女体化"]
      names->genre-ids))

;; https://www.dmm.co.jp/digital/videoa/-/list/=/article=keyword/id=2007/
(def fat-ids
  (-> ["ぽっちゃり"]
      names->genre-ids))

(def ng-genres
  (into #{} (concat
             antisocial-ids
             violent-ids
             dirty-ids
             trans-ids)))
