(ns tools.twitter
  (:require
   [devtools :refer [->screen-name ->user-id kotori-params]]
   [firebase :refer [db-prod]]
   [kotori.procedure.kotori.core :as kotori]))

(defn delete-tweet! [db code tweet-id]
  (let [params (kotori-params db code)]
    (kotori/delete-tweet!
     (assoc params :tweet-id tweet-id))))

#_(delete-tweet! (db-prod) "0027" "")
