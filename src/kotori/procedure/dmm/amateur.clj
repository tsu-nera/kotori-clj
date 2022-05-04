(ns kotori.procedure.dmm.amateur
  (:require
   [kotori.lib.provider.dmm.api :as api]))

(comment
  (require '[tools.dmm :refer [dmm-creds]])

  (def resp (api/search-product (dmm-creds) {:cid   "smuc029"
                                             :floor "videoc"}))
  )
