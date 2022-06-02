(ns tools.dmm.api
  "DMM APIの呼び出しplaygrownd"
  (:require
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.product :as lib]
   [tools.dmm :refer [creds]]))

(comment
  (def products
    (lib/get-products {:creds      (creds)
                       :sort       "rank"
                       :floor      "videoa"
                       :article    "genre"
                       :article_id 1031
                       }))
  (map :title products)

  )

(comment

  ;; directorによる絞り込み
  (def directors
    (lib/get-products {:creds      (creds)
                       :sort       "rank"
                       :floor      "videoa"
                       :article    "director"
                       :article_id 114124}))

  (def keywords
    (lib/get-products {:creds   (creds)
                       :sort    "rank"
                       :floor   "videoa"
                       :keyword "さもあり"
                       }))

  (->> keywords
       (map :title))

  )

(comment
  (def resp (api/search-author (creds) {:floor_id 43
                                        :initial  "あ"
                                        }))

  )
