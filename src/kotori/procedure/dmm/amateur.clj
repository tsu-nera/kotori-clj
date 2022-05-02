(ns kotori.procedure.dmm.amateur
  (:require
   [kotori.lib.provider.dmm.api :as api]))

(comment
  (require '[tools.dmm :refer [dmm-creds]])

  (def creds (api/map->Credentials (dmm-creds)))

  (def resp (api/search-product creds {:cid   "smuc029"
                                       :floor "videoc"}))
  )
