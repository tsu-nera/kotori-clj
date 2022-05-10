(ns kotori.lib.kotori
  (:require
   [clojure.string :as str]
   [kotori.domain.config.ngword :refer [source]]
   [kotori.lib.provider.dmm.parser :as p]
   [kotori.lib.twittertext :as tt]))

(defn desc->headline [text]
  (let [re (re-pattern "^＜(.+?)＞|^【(.+?)】")]
    (first (re-find re text))))

(defn desc->dialogue [text]
  (let [re (re-pattern "「(.+?)」")]
    (when-let [dialogue (first (re-find re text))]
      (if (< 6 (count dialogue))
        dialogue))))

(defn trim-headline [text]
  (if-let [headline (desc->headline text)]
    (-> text
        (str/replace headline ""))
    text))

(defn remove-bodysize [text]
  (let [re (re-pattern "T(.+)cm")]
    (if-let [target (first (re-find re text))]
      (-> text
          (str/replace target ""))
      text)))

(defn- ->last-char [s]
  (let [n (count s)]
    (cond-> (str s)
      (not (zero? n)) (subs 0 (- n 1)))))

(defn- drop-last-char [s]
  (subs s 0 (- (count s) 1)))

(defn remove-last-x [x s]
  (let [last-char (->last-char s)]
    (if (= last-char x)
      (str/trim (drop-last-char s))
      s)))

(defn add-tenten [text]
  (let [last-char (->last-char text)]
    (cond
      (= last-char "。") (str (drop-last-char text) "…")
      (= last-char "！") text
      (= last-char "？") text
      :else              (str text "…"))))

(defn- normalize
  "全角を半角に変換してツイート文字数を稼ぐ"
  [s]
  (-> s
      (str/replace #"！" "!")
      (str/replace #"？" "?")
      (str/replace #"。" ".")
      (str/replace #"、" ", ")))

(defn desc->sentences [text]
  (-> text
      (str/replace #"。。。" ".")
      (str/replace #"。" "。\n")
      (str/replace #"！！" "！！\n")
      (str/replace #"！？" "？\n")
      (str/replace #"…！" "…！\n")
      (str/split-lines)))

(defn desc->trimed
  [text  & {:keys [length] :or {length 100}}]
  (and text
       (->> text
            trim-headline
            remove-bodysize
            p/->sentences
            (map str/trim)
            ((partial p/join-sentences length))
            ((partial remove-last-x "【")) ;; fsデータにゴミがはいったので
            add-tenten
            normalize)))

(defn ng->ok [text]
  (when text
    (reduce (fn [x [k v]]
              (str/replace x k v)) text source)))

(defn- ->add-chan [name]
  (if (str/includes? name "さん")
    name
    (if (not (str/includes? name "ちゃん"))
      (str name "ちゃん")
      name)))

(defn- ->remove-annonymous [name]
  ((partial p/->remove-x "（仮名）") name))

(defn- ->remove-num [name]
  (-> name
      (str/replace #" 3| 2" "")))

(defn- ->swap-local-wife [name]
  (str/replace name #"ローカル妻" "匿名希望の奥さん"))

(defn videoc-title->name [title]
  (-> title
      ->remove-num
      ->remove-annonymous
      ->swap-local-wife
      (str/replace #"天才" "")
      ->add-chan))

(defn- ->remove-haishin [s]
  ((partial p/->remove-x "【配信限定特典映像付き】") s))

(defn- ->remove-4k-headline [s]
  ((partial p/->remove-x "【圧倒的4K映像でヌク！】") s))

(defn ->actress-names [product]
  (map (fn [m] (get m "name")) (:actresses product)))

(defn ->title-without-actress [product]
  (->> product
       ((juxt :title ->actress-names))
       ((fn [[title names]]
          (reduce (fn [title name]
                    (let [re (re-pattern (str name "$"))]
                      (str/replace title re "")))
                  title names)))))

(defn ->titles-without-actress [products]
  (map ->title-without-actress products))

(defn- title->trimed [title]
  (-> title
      p/remove-hashtags
      ->remove-haishin
      ->remove-4k-headline
      str/trim))

(defn ->next
  [product]
  (let [cid                   (:cid product)
        title-raw             (:title product)
        title-without-actress (->title-without-actress product)
        title                 (-> title-without-actress
                                  ng->ok
                                  title->trimed)
        desc-raw              (:description product)
        hashtags              (p/->hashtags title-raw)
        desc                  (-> desc-raw ng->ok desc->trimed)
        summary               (-> (:summary product) ng->ok)]
    {:cid             cid
     :title           title
     :title-raw       title-raw
     :description     desc
     :description-raw desc-raw
     :summary         summary
     :hashtags        hashtags
     ;; :headline    (desc->headline desc)
     ;; :dialogue    (desc->dialogue desc)
     }))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment

  (require '[firebase :refer [db-prod db-dev db]]
           '[devtools :refer [->screen-name env]]
           '[kotori.procedure.strategy.dmm
             :refer [select-scheduled-products]])

  (def screen-name (->screen-name "0001"))
  (def products
    (into []
          (select-scheduled-products {:db          (db-prod)
                                      :limit       20
                                      :screen-name screen-name})))
  (def descs (map :description products))

  (map desc->dialogue descs)
  (map desc->headline descs)
  (println (nth (map desc->trimed descs) 2))

  (def trimed (map desc->trimed descs))
  (def sample "女神の美体から汗、涎、愛液、潮…全エキスが大・放・出！体液まみれでより一層エロさを増した美乃すずめが快楽のまま本能全開で汁だくSEX！絶頂に次ぐ絶頂、意識朦朧となるほどの本気の交わりで大量失禁＆大量イキ潮スプラッシュ！全身ぐっちょり、体液滴るイイ女が性欲尽きるまでイッてイッてイキまくる！！")
  (def ret (desc->trimed sample))

  (def desc2 "色白でスレンダーな潮美舞の身体を専属4本目でここまでやるのかというくらい徹底的に性感開発！彼女の名前の由来となった潮吹きをベッドが水たまりができるまで何度も何度も出してもらいました。アバラ浮き出るほどの激ピス、焦らしからの急変連続イカセ、立ち拘束で脚ガクガクになるまで玩具責め、追撃3P…全コーナー絶頂痙攣しまくり潮吹きまくりの見どころ満載！細い子が大絶頂する姿は何故こんなに興奮するのでしょう…。")
  (def ret (desc->trimed desc2))

  (def desc3 "いつもテレビで観ていたあの女子アナがまさかの隣人…！？あざとカワイイ成田つむぎに誘惑されまくる僕！ずっとファンだった！こんなあざとカワイイ誘惑、我慢できるわけもなくて…隣の部屋に妻がいるのにどんどん大胆にエロくなっていくつむぎさんの誘惑にイチコロ。小悪魔淫語とあざとエロいテクで僕は何回も射精させられちゃう…お茶の間騒然の中出し不倫SEX！「あたし…こんなことバレたら番組降板になっちゃう（照）」。")

  (def desc5 "T170cmB99cmW58cmH88cm圧倒的カラダを持つ新人グラビアアイドル‘山手梨愛’が遂に本当の絶頂を知る今作品！もともとウブだった彼女が恥じらいも他人の目もどうでもよくなるように禁欲、媚薬オイル、玩具ガン責め、人生で一番の激ピストンと快感の大洪水で理性決壊！！九州ナンバーワンの神ボディが性感帯バグを起こして「イグイグイッヂャウゥ～」。超ド級の美体アクメ姿は必見です！！")

  (def desc6 "こんな子がAVに出演するとは思えない。清楚で知的な現役女子大生の気象予報士の卵‘白坂みあん’がAVデビュー！")

  (def desc7 "学校のマドンナ的存在だった麻耶は同じ学校の晃司と結婚し、幸せな生活を送っていた。そんな中、同窓会の知らせが届き、二人が参加した。その同窓会は麻耶に憧れていた根暗な同級生たちが仕組んだ罠だった。睡眠薬を盛られて晃司が寝てしまい、残された麻耶は同級生たちに組み敷かれていく。麻耶は媚薬を盛られ、自ら挿入を懇願するスケベ女に成り果てる。その姿を動画に撮られ、強請られた麻耶は同級生たちの性奴●へと化して…。")

  (def desc8 " ジュポジュポイラマで口内奉仕/ビンビン乳首をツネあげられて腰砕け昇天お漏らし/唾液ダラダラ垂らしながらデカチンズップシイキまくりSEX/チ○ポを咥えながら興奮してお漏らししちゃう変態デカ尻バニー/淫乱マ○コ突かれて絶叫イキ狂い大量潮吹きSEX/ビショビショお漏らししながらイキまくり/ムチムチ淫乱バニーが勃起チ○ポたっぷりご奉仕でザーメン抜きまくり/テカテカ肉感巨乳デカ尻バニーイキまくり中出しSEX")

  (desc->sentences sample)

  (def xs (p/->sentences sample))

  (def ret (desc->trimed sample))

  (p/test! (desc->trimed desc8) desc8)
  )

(comment
  (require
   '[clojure.string :as str]
   '[kotori.procedure.dmm.amateur
     :refer [select-scheduled-products]
     :rename {select-scheduled-products select-scheduled-videocs}])

  (def screen-name (->screen-name "0001"))
  (def products
    (into []
          (select-scheduled-products {:db          (db-prod)
                                      :limit       50
                                      :screen-name screen-name})))

  (->titles-without-actress products)

  #_(defn products->actress-names [products]
      (let [actresses (map :actresses products)]
        (map (fn [xm]
               (map (fn [m] (get m "name")) xm)) actresses)))

  (def titles (map :title products))

  (def names (into [] (map title->name titles)))

  )

(comment
  (def title "【配信限定特典映像付き】朝ドラ系現役アイドルT○kT○ker 西元めいさ 初体験で初絶頂 ＃初イキ ＃初巨根 ＃初3P ＃キャパオーバー ＃快感 ＃くびれボディ ＃ビクッビク【圧倒的4K映像でヌク！】")
  (def title2 "【VR】シン・時間停止 ―女湯イタズラ大戦争VR―")

  (title->trimed title)
  )
