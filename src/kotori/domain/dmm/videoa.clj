(ns kotori.domain.dmm.videoa
  "
  refs.
  - https://www.dmm.co.jp/digital/videoa/-/genre/
  - https://affiliate.dmm.com/api/v3/genresearch.html")

(def amateur-ids
  #{;
    4024 ; 素人
    4006 ; ナンパ
    6002 ; ハメ撮り
    })

;; 2022.05現在, VR専用はハイクオリティVRを内包する
(def vr-only-id 6793)

(def vr-ids
  #{;
    6793 ;; VR専用
    6925 ;: ハイクオリティVR
    })

(def antisocial-ids
  "Twitter的にダメそうなジャンル."
  #{;
    4021 ; 盗撮・のぞき
    5015 ; ドラッグ
    })

(def violent-ids
  #{21 567 5059 6094 6953})

(def dirty-ids
  #{4018 5007 5011 5012 5013 5014 5024 6151})

(def trans-ids
  #{3036 4015})

(def ng-genres
  (into #{} (concat
             antisocial-ids
             violent-ids
             dirty-ids
             trans-ids)))
