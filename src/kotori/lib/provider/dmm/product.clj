(ns kotori.lib.provider.dmm.product
  (:require
   [kotori.lib.provider.dmm.api :as api]))

(defn get-videoc [{:keys [cid creds]}]
  (when-let [resp (api/search-product
                   creds {:floor "videoc" :cid cid})]
    (first resp)))

(defn get-videoa [{:keys [cid creds]}]
  (when-let [resp (api/search-product
                   creds {:floor "videoa" :cid cid})]
    (first resp)))

#_(defn get-videocs
    [{:keys [env offset hits keyword article article-id]
      :or   {offset 1 hits 100}}]
    {:pre [(<= hits 100)]}
    (let [{:keys [api-id affiliate-id]}
          env
          creds (api/->Credentials api-id affiliate-id)
          req   (cond->
                    {:offset offset :sort "rank" :hits hits}
                  keyword    (assoc :keyword keyword)
                  article    (assoc :article article)
                  article-id (assoc :article_id article-id))
          items (api/search-product creds req)]
      items))

(defn- ->genre-req [genre-id]
  {:article "genre" :article_id genre-id})

(defn get-by-genre [{:keys [genre-id creds]}]
  (let [q (->genre-req genre-id)]
    (->> (api/search-product creds q)
         (into []))))

(defn get-by-genres
  "複数genre-idをパラレルで取得して結果をマージ."
  [{:keys [genre-ids creds]}]
  (->> genre-ids
       (map #(->genre-req %))
       (pmap #(api/search-product creds %))
       flatten
       (into #{})
       (into [])))

(comment
  (require '[tools.dmm :refer [dmm-creds]])
  (def creds (dmm-creds))

  (def ret (get-videoc {:creds creds :cid "smuc029"}))
  (def ret (get-videoa {:creds creds :cid "mism00237"}))

  (def resp (api/search-product
             creds {:article "genre" :article-id 6793}))

  (def resp (->> [6793 6925]
                 (map #(->genre-req %))
                 (pmap #(api/search-product creds %))
                 flatten
                 (into #{})
                 (into [])
                 ))

  )
