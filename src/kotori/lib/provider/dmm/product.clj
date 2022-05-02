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

(comment
  (require '[tools.dmm :refer [dmm-creds]])
  (def creds (api/map->Credentials (dmm-creds)))

  (def ret (get-videoc {:creds creds :cid "smuc029"}))
  (def ret (get-videoa {:creds creds :cid "mism00237"}))
  )
