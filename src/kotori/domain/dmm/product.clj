(ns kotori.domain.dmm.product
  (:require
   [clojure.string :as string]
   [kotori.lib.json :as json]
   [kotori.lib.time :as time]))

(defn- ->cid [raw]
  (:content_id raw))

(defn- ->title [raw]
  (:title raw))

(defn- ->url [raw]
  (:URL raw))

(defn- ->affiliate-url [raw]
  (:affiliateURL raw))

(defn- ->actresses [raw]
  (get-in raw [:iteminfo :actress]))

(defn- ->performer [raw]
  (string/join
   "," (map #(:name %) (->actresses raw))))

(defn- ->released-time [raw]
  (let [date-str (:date raw)]
    (-> date-str
        (time/parse-dmm-timestamp)
        (time/->fs-timestamp))))

(defn- ->genres [raw]
  (get-in raw [:iteminfo :genre]))

(defn- ->genre [raw]
  (string/join
   "," (map #(:name %) (->genres raw))))

(defn- ->timestamp [_] (time/->fs-timestamp (time/now)))

(defn ->legacy [raw]
  {:cid         (->cid raw)
   :title       (->title raw)
   :url         (->affiliate-url raw)
   :performer   (->performer raw)
   :released_at (->released-time raw)
   :created_at  (->timestamp raw)
   :updated_at  (->timestamp raw)
   :genre       ()
   :status      "READY"
   :ranking     0
   :posted_at   nil
   :tweet_link  nil
   :tweet_id    nil})

(defn ->data [raw]
  "dmm response map -> firestore doc mapの写像"
  (let [data   {:cid           (->cid raw)
                :title         (->title raw)
                :url           (->url raw)
                :affiliate_url (->affiliate-url raw)
                :actresses     (->actresses raw)
                :released_time (->released-time raw)
                :genres        (->genres raw)
                :created_time  (->timestamp raw)
                :updated_time  (->timestamp raw)}
        legacy (->legacy raw)]
    (-> data
        (assoc :raw raw)
        (assoc :legacy legacy)
        (json/->json))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (require '[local :refer [env db]])
  (require '[kotori.procedure.dmm :refer [get-product]])
  (def raw
    (get-product {:cid "ssis00335" :env (env)}))

  (def actresses (->actresses raw))
  (map #(:name %) actresses)

  (def data (->data raw))
  )

(comment
  (require '[portal.api :as p])
  (def d (p/open))
  (reset! d (->data (get-product {:cid "ssis00335" :env (env)})))
  @d
  )
