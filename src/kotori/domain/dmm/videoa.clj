(ns kotori.domain.dmm.videoa
  "refs.https://www.dmm.co.jp/digital/videoa/-/genre/"
  (:require
   [kotori.domain.dmm.core :as core]
   [kotori.lib.io :as io]))

(def genre-path "dmm/genre/videoa.edn")
(defonce genres (->> genre-path io/load-edn))

(def genre-name-id-map (core/genres->name-id-map genres))
(def genre-id-name-map (core/genres->id-name-map genres))
(def genre-names->ids (partial core/names->genre-ids genre-name-id-map))

(def amateur-ids
  (-> ["素人" "ナンパ" "ハメ撮り"]
      genre-names->ids))

;; 2022.05現在, VR専用はハイクオリティVRを内包する
(def vr-only-id (get genre-name-id-map "VR専用"))

(def vr-ids
  (-> ["VR専用" "ハイクオリティVR"]
      genre-names->ids))

(def antisocial-ids
  (-> ["盗撮・のぞき" "ドラッグ"]
      genre-names->ids))

(def violent-ids
  (-> ["残虐表現" "鬼畜" "拷問" "蝋燭" "鼻フック"]
      genre-names->ids))

(def dirty-ids
  (-> ["スカトロ" "食糞" "放尿・お漏らし"
       "飲尿" "脱糞" "浣腸" "ゲロ" "異物挿入"]
      genre-names->ids))

(def trans-ids
  (-> ["女装・男の娘" "ニューハーフ" "ふたなり"
       "ゲイ" "性転換・女体化"]
      genre-names->ids))

(def ng-genres
  (into #{} (concat
             antisocial-ids
             violent-ids
             dirty-ids
             trans-ids)))
