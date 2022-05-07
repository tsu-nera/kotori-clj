(ns kotori.procedure.dmm.amateur
  (:require
   [kotori.domain.dmm.videoa
    :refer [amateur-genre-id]
    :rename {amateur-genre-id videoa-id}]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.product :as lib]))

;; 素人ジャンルはvideocに属するものとvideoaに属するジャンルがある.
;; 素人ジャンル作品として女優を利用していればvideoa
;; 本物の素人はvideoc

(comment
  (require '[devtools :refer [env ->screen-name]]
           '[tools.dmm :refer [creds]]
           '[firebase :refer [db-prod db-dev db]])

  (def product (lib/get-videoc {:creds @creds
                                :cid   "smuc029"}))
  (def product (lib/get-videoa {:creds @creds
                                :cid   "1kmhrs00044"}))

  (def resp (api/search-product @creds {:cid   ""
                                        :floor "videoc"}))
  )
