(ns tools.twitter
  (:require
   [devtools :refer [->screen-name ->user-id kotori-params twitter-auth]]
   [firebase :refer [db-prod]]
   [kotori.procedure.kotori.core :as kotori]
   [twitter-clj.private :as private]))

(defn delete-tweet! [db code tweet-id]
  (let [params (kotori-params db code)]
    (kotori/delete-tweet!
     (assoc params :tweet-id tweet-id))))

#_(delete-tweet! (db-prod) "0027" "")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[twitter-clj.private :as private])

  (def sample-path "../../Downloads/d_227233jp-001.jpg")

  (private/->total-bytes sample-path)
  (private/create-tweet (twitter-auth) {:text "test3"})

  (def resp
    (private/upload-media (twitter-auth) {:user-id   (->user-id "0003")
                                          :file-path sample-path}))
  )
