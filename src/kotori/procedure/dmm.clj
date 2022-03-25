(ns kotori.procedure.dmm
  (:require
   [firestore-clj.core :as f]
   [kotori.domain.dmm.product :as product]
   [kotori.lib.provider.dmm :as client]))

(defn get-product [{:keys [cid env]}]
  (let [{:keys [api-id affiliate-id]} env
        creds
        (client/->Credentials api-id affiliate-id)
        resp
        (client/search-product creds {:cid cid})]
    (-> resp
        (:result)
        (:items)
        (first))))

(defn crawl-product [{:keys [db cid] :as m}]
  "Get and save to Firestore."
  (let [product (get-product m)
        data    (product/->doc-data product)
        path    (str "providers/dmm/products/" cid)]
    (-> db
        (f/doc path)
        (f/set! data))))

(comment
  (require '[local :refer [env db]])
  (def product (get-product {:cid "ssis00337" :env (env)}))

  (tap> product)

  (:content_id product)

  (product/->obj product)

  (defn tmp [{:keys [cid db] :as m}]
    (let [path (str "providers/dmm/products/" cid)]
      path))
  (f/doc (db) "providers/dmm/products/ssis00337")

  (crawl-product {:cid "ssis00337" :env (env) :db (db)})
  )
