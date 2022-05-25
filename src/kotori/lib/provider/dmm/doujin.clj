(ns kotori.lib.provider.dmm.doujin
  (:require
   [kotori.lib.io :as io]
   [kotori.lib.provider.dmm.api :as api]
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

(def service-code (:code fanza-doujin))
(def floor-code "digital_doujin")
(def floor-id 81)

(def base-req-opts
  {:service service-code
   :floor   floor-code})

(defn get-product [{:keys [cid creds]}]
  (when-let [resp (api/search-product
                   creds (-> base-req-opts
                             (assoc :cid cid)))]
    (first resp)))

(defn get-products [{:keys [creds] :as m}]
  (when-let [resp (api/search-product
                   creds (merge m base-req-opts))]
    resp))

;; "https://pics.dmm.co.jp/digital/cg/d_205949/d_205949pt.jpg"
(defn ->format [resp]
  (let [url (get-in resp [:imageURL :list])
        re  (re-pattern
             "digital/(cg|voice|game|comic)/d_")]
    (second (re-find re url))))

(defn ->url [cid]
  (str "https://www.dmm.co.jp/dc/doujin/-/detail/=/cid=" cid "/"))

(defn ->pr-url [urls]
  (first urls))

(defn ->jp-urls [urls]
  (into [] (rest urls)))

;; cidからパスが推測できるならいらいなかもしれない.
;; 法則性がある程度はっきりしたら検討
;;
(defn get-image-urls
  [cid]
  (let [page-url (->url cid)]
    (when-let [resp (public/get-page-data page-url)]
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

(comment
  (require '[tools.dmm :refer [creds dump-doujin!]])

  (def cid "d_229101") ;; CG cg
  (def cid "d_223288") ;; デモムービー動画ジャンルだけどCG
  (def cid "d_230940");; コミック comic
  (def cid "d_226142") ;; ゲームgame
  (def cid "d_217813") ;; 音声 voice
  (def cid "d_cos0027") ;; コスプレ動画 genreid=156007

  (def resp (get-product {:cid cid :creds (creds)}))
  (dump-doujin! cid)

  (->format resp)
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
