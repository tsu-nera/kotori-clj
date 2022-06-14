(ns tools.tweet
  "主にfirestoreのtweets collectionをいじるツールをおく"
  (:require
   [devtools :refer [->screen-name ->user-id kotori-params]]
   [firebase :refer [db-prod]]
   [firestore-clj.core :as f]
   [kotori.domain.tweet.core
    :refer [->retweet-coll-path
            ->unretweet-coll-path
            ->post-coll-path
            ->post-doc-path]]
   [kotori.lib.firestore :as fs]
   [kotori.lib.time :refer [fs-now]]))

(defn- disable-self-retweet! [user-id tweet-id]
  (fs/merge! (db-prod)
             (->post-doc-path user-id tweet-id)
             {"self_retweet" false
              "updated_at"   (fs-now)}))

(defn disable-self-retweets! [code]
  (let [user-id     (->user-id code)
        post-ids    (->> (fs/get-filter-docs
                          (db-prod)
                          (->post-coll-path user-id)
                          {"self_retweet" true})
                         (map :tweet-id))
        retweet-ids (->> (fs/get-docs
                          (db-prod)
                          (->retweet-coll-path user-id))
                         (map :tweet-id))
        diff-ids    (into [] (clojure.set/difference
                              (into #{} post-ids)
                              (into #{} retweet-ids)))]
    (doseq [tweet-id diff-ids]
      (disable-self-retweet! user-id tweet-id))))
#_(disable-self-retweets! "0020")
