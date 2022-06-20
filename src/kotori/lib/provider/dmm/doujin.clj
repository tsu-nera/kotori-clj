(ns kotori.lib.provider.dmm.doujin
  (:require
   [kotori.domain.dmm.genre.doujin :as genre]
   [kotori.domain.dmm.product :as product]
   [kotori.lib.io :as io]
   [kotori.lib.json :as json]
   [kotori.lib.provider.dmm.api :as api]
   [kotori.lib.provider.dmm.product :as lib]
   [kotori.lib.provider.dmm.public :as public]
   [net.cgrand.enlive-html :as html]))

;; APIで取得した情報を転機
(def fanza-doujin
  {:name  "同人",
   :code  "doujin",
   :floor [{:id "81", :name "同人", :code "digital_doujin"}]})

;; APIには現れないが以下にサブフロアが存在する
;; これはimageURLのパスの分類から推測した.
(def media
  {:comic "comic"
   :game  "game"
   :cg    "cg"
   :voice "voice"})

;; FANZAのURLをみるとsectionはこの３種類に分けられている.
(def section
  {:mens "mens"
   :tl   "tl"
   :bl   "bl"})

(def service-code (:code fanza-doujin))
(def floor-code "digital_doujin")
(def floor-id 81)

(def base-req-opts
  {:service service-code
   :floor   floor-code})

(defn for-boy? [genre-ids]
  (.contains genre-ids genre/for-boy-id))

(defn for-girl? [genre-ids]
  (.contains genre-ids genre/for-girl-id))

(defn bl? [genre-ids]
  (.contains genre-ids genre/bl-id))

(defn gay? [genre-ids]
  (.contains genre-ids genre/gay-id))

;; 実際にTLラベルがついているものが少ない気がする
(defn tl? [genre-ids]
  (and (for-girl? genre-ids)
       (not (bl? genre-ids))
       (not (gay? genre-ids))))

(defn get-product [{:keys [cid creds]}]
  (when-let [resp (api/search-product
                   creds (-> base-req-opts
                             (assoc :cid cid)))]
    (first resp)))

(defn get-products [{:as m}]
  (when-let [resp (lib/get-products (merge m base-req-opts))]
    resp))

(defn get-boys-products [{:as m}]
  (when-let [products (get-products
                       (assoc m :genre-id genre/for-boy-id))]
    products))

(defn get-girls-products [{:as m}]
  (when-let [products (get-products
                       (assoc m :genre-id genre/for-girl-id))]
    products))

;; "https://pics.dmm.co.jp/digital/cg/d_205949/d_205949pt.jpg"
(defn ->format [resp]
  (let [url (get-in resp [:imageURL :list])
        re  (re-pattern
             "digital/(cg|voice|game|comic)/d_")]
    (second (re-find re url))))

;; 人気順で取得すると音声作品は人気がないので取得できない.
;; そのため音声作品を特定できるgenre-id指定で取得する.
;; これだけでOKというgenre-idがないので複数個のgenre-idで取得して
;; 結果をマージする.
(defn get-voice-products [{:keys [creds limit] :or {limit 100}}]
  (when-let [products (lib/get-by-genres
                       genre/voice-ids
                       (merge base-req-opts {:creds creds
                                             :hits  limit}))]
    (filter (fn [p]
              (= "voice" (->format p))) products)))

(defn api->data
  "dmm response map -> firestore doc mapの写像"
  [raw]
  (let [data {:cid           (product/->cid raw)
              :title         (product/->title  raw)
              :url           (product/->url raw)
              :affiliate_url (product/->affiliate-url raw)
              :released_time (product/->released-time raw)
              :genres        (product/->genres raw)
              :format        (->format raw)}]
    (-> data
        (assoc :raw raw))))

(defn ->url [cid]
  (str "https://www.dmm.co.jp/dc/doujin/-/detail/=/cid=" cid "/"))

(defn ->pr-url [urls]
  (first urls))

(defn ->jp-urls [urls]
  (into [] (rest urls)))

;; cidからパスが推測できるならいらいなかもしれない.
;; 法則性がある程度はっきりしたら検討
(defn get-image-urls
  [cid]
  (let [page-url (->url cid)]
    (when-let [resp (public/get-page-raw page-url)]
      (->
       (->> (html/select resp [:a.fn-colorbox])
            (map (fn [item] (get-in item [:attrs :href]))))))))

;; 0. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-001.jpg"
;; 1. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-002.jpg"
;; 2. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-003.jpg"
;; 3. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-004.jpg"
;; 4. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-005.jpg"
;; 5. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-006.jpg"
;; 6. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-007.jpg"
;; 7. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-008.jpg"
;; 0. "https://doujin-assets.dmm.co.jp/digital/cg/d_229101/d_229101jp-001.jpg"
;; 1. "https://doujin-assets.dmm.co.jp/digital/cg/d_229101/d_229101jp-002.jpg"
;; 2. "https://doujin-assets.dmm.co.jp/digital/cg/d_229101/d_229101jp-003.jpg"
;; 3. "https://doujin-assets.dmm.co.jp/digital/cg/d_229101/d_229101jp-004.jpg"
(defn generate-image-url [media cid n]
  (str "https://doujin-assets.dmm.co.jp/digital/"
       media "/" cid "/" cid "jp-00" n ".jpg"))

(defn generate-image-urls [media cid count]
  (map (fn [n]
         (generate-image-url media cid n)) (range 1 (+ 1 count))))

(defn get-voice-urls
  [cid]
  (let [page-url (->url cid)
        xf       (comp
                  (map :content)
                  (map second)
                  (map (fn [item] (get-in item [:attrs :src]))))]
    (when-let [raw (public/get-page-raw page-url)]
      (->> (html/select raw [:video]) (into [] xf)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (require '[tools.dmm :refer [creds dump-doujin!]])
  )

(comment

  (def cid "d_229101") ;; CG cg
  (def cid "d_223288") ;; デモムービー動画ジャンルだけどCG
  (def cid "d_230940");; コミック comic
  (def cid "d_226142") ;; ゲームgame
  (def cid "d_217813") ;; 音声 voice
  (def cid "d_cos0027") ;; コスプレ動画 genreid=156007

  (def resp (get-product {:cid cid :creds (creds)}))
  (dump-doujin! cid)

  (def doujins (get-products {:creds (creds) :limit 200}))
  (count doujins)
  )

(comment
  (def girls (get-products {:creds    (creds)
                            :limit    300
                            :genre-id genre/for-girl-id}))
  (count girls)

  (def bls (filter bl? girls))
  (count bls)

  (def tls (filter tl? girls))
  (count tls)
  )

(comment
  (def cid "d_231127")  ;; BL
  (def cid "d_231143")  ;; TL

  (def product (get-product {:cid cid :creds (creds)}))
  (dump-doujin! cid)

  (def doujins (get-products {:creds (creds) :limit 300}))

  (def for-girls (filter for-girl? doujins))
  (count for-girls)

  (def for-boys (filter for-boy? doujins))
  (count for-boys)
  )

(comment
  (def cid "d_227233")

  (def url (->url cid))

  (def ret (get-image-urls cid))

  (def urls (->jp-urls ret))
  ;; 0. "https://doujin-assets.dmm.co.jp/digital/cg/d_229101/d_229101jp-001.jpg"
  ;; 1. "https://doujin-assets.dmm.co.jp/digital/cg/d_229101/d_229101jp-002.jpg"
  ;; 2. "https://doujin-assets.dmm.co.jp/digital/cg/d_229101/d_229101jp-003.jpg"
  ;; 3. "https://doujin-assets.dmm.co.jp/digital/cg/d_229101/d_229101jp-004.jpg"
  ;;
  ;; 0. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-001.jpg"
  ;; 1. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-002.jpg"
  ;; 2. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-003.jpg"
  ;; 3. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-004.jpg"
  ;; 4. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-005.jpg"
  ;; 5. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-006.jpg"
  ;; 6. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-007.jpg"
  ;; 7. "https://doujin-assets.dmm.co.jp/digital/comic/d_227233/d_227233jp-008.jpg"

  (def image-url (first urls))

  (io/download! image-url)
  (io/downloads! urls)
  )

(comment
  (def cid "d_227233")
  (def urls (generate-image-urls "comic" cid 3))
  )

(comment
  (def cid "d_214569")
  (def url (->url cid))
  (def raw (public/get-page-raw url))

  (def urls (get-voice-urls cid))

  (def voices (get-voice-products {:creds (creds)}))
  )
