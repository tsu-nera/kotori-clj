(ns kotori.lib.provider.dmm.doujin
  (:require
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.public :as public]
   [net.cgrand.enlive-html :as html]))

;; APIで取得した情報を転機
(def fanza-doujin
  {:name  "同人",
   :code  "doujin",
   :floor [{:id "81", :name "同人", :code "digital_doujin"}]})

(def service-code (:code fanza-doujin))
(def floor-code "digital_doujin")
(def floor-id 81)

(def base-req-opts
  {:service service-code
   :floor   floor-code})

(defn get-doujin [{:keys [cid creds]}]
  (when-let [resp (api/search-product
                   creds (-> base-req-opts
                             (assoc :cid cid)))]
    (first resp)))

;; "https://pics.dmm.co.jp/digital/cg/d_205949/d_205949pt.jpg"
(defn ->format [resp]
  (let [url (get-in resp [:imageURL :list])
        re  (re-pattern
             "digital/(cg|voice|game|comic)/d_")]
    (second (re-find re url))))

(defn ->url [cid]
  (str "https://www.dmm.co.jp/dc/doujin/-/detail/=/cid=" cid "/"))

(defn get-image-urls "
  末尾でリストにjuxtをあてることで第一戻り値はサムネイル画像URL,
  残りはサンプルURLのリストとなる."
  [cid]
  (let [page-url (->url cid)]
    (when-let [resp (public/get-page-data page-url)]
      (->
       (->> (html/select resp [:a.fn-colorbox])
            (map (fn [item] (get-in item [:attrs :href]))))
       ((juxt first
              #(into [] (rest %))))))))

(comment
  (require '[tools.dmm :refer [creds dump-doujin!]])

  (def cid "d_229101") ;; CG cg
  (def cid "d_223288") ;; デモムービー動画ジャンルだけどCG
  (def cid "d_230940");; コミック comic
  (def cid "d_226142") ;; ゲームgame
  (def cid "d_217813") ;; 音声 voice
  (def cid "d_cos0027") ;; コスプレ動画 genreid=156007

  (def resp (get-doujin {:cid cid :creds (creds)}))
  (dump-doujin! cid)

  (->format resp)
  )

(comment
  (def cid "d_230940")
  ;; https://www.dmm.co.jp/dc/doujin/-/detail/=/cid=d_227233/
  (def url (->url cid))
  (def resp (public/get-page-data url))

  (def ret (get-image-urls cid))

  )
