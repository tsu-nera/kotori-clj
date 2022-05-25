(ns tools.twitter
  (:require
   [clojure.java.io :as io]
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
  (require '[twitter-clj.media :as media])

  (def sample-path "../../Downloads/d_227233jp-001.jpg")

  (private/->total-bytes sample-path)
  (private/create-tweet (twitter-auth) {:text "test3"})

  (def resp-init
    (media/init {:creds     (twitter-auth)
                 :file-path sample-path}))

  (def media-id (:media-id resp-init))

  (def resp-append
    (media/append {:creds     (twitter-auth)
                   :media-id  (:media-id resp-init)
                   :file-path sample-path}))

  (def resp-finalize
    (media/finalize {:creds    (twitter-auth)
                     :media-id media-id}))

  ;; 画像の場合はエラー.
  (def resp-status
    (media/status {:creds    (twitter-auth)
                   :media-id media-id}))

  (def ret (private/upload-image {:creds     (twitter-auth)
                                  :file-path sample-path}))

  (private/create-tweet (twitter-auth) {:text      "test4"
                                        :media-ids [(:media-id ret)]})
  )
