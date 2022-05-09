(ns kotori.lib.kotori
  (:require
   [clojure.string :as str]
   [kotori.domain.config.ngword :refer [source]]
   [kotori.lib.provider.dmm.parser :as p]))

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
  (subs s
        (- (count s) 1)))

(defn add-tententen [text]
  (let [last-char (->last-char text)]
    (cond
      (= last-char "。") text
      (= last-char "！") text
      (= last-char "？") text
      :else              (str text "..."))))

(defn desc->sentences [text]
  (-> text
      (str/replace #"。。。" "。")
      (str/replace #"。" "。\n")
      (str/replace #"！！" "！！\n")
      (str/replace #"！？" "？\n")
      (str/replace #"…！" "…！\n")
      (str/split-lines)))

(defn- trunc
  [s n]
  (subs s 0 (min (count s) n)))

(defn join-sentences [sentences & {:keys [length] :or {length 80}}]
  (-> (if (= 1 (count sentences))
        (trunc (first sentences) length)
        (reduce (fn [desc sentence]
                  (if (< length (+ (count desc) (count sentence)))
                    (if (zero? (count desc))
                      (trunc sentence length)
                      desc)
                    (str desc "\n\n" sentence)))
                "" sentences))
      str/trim))

(defn desc->trimed
  [text]
  (and text
       (-> text
           trim-headline
           remove-bodysize
           desc->sentences
           join-sentences
           add-tententen)))

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

(defn- title->trimed [title]
  (-> title
      p/->remove-hashtags
      ->remove-haishin
      ->remove-4k-headline
      str/trim))

(defn ->next
  [product]
  (let [cid       (:cid product)
        title-raw (:title product)
        title     (-> title-raw ng->ok title->trimed)
        desc-raw  (:description product)
        hashtags  (p/->hashtags title-raw)
        desc      (-> desc-raw ng->ok desc->trimed)
        summary   (-> (:summary product) ng->ok)]
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

  (re-find (re-pattern "T(.+)cm") desc5)

  (remove-bodysize desc5)

  (desc->sentences desc5)
  (def ret (desc->trimed desc3))
  )

(comment
  (require
   '[clojure.string :as str]
   '[kotori.procedure.dmm.amateur
     :refer [select-scheduled-products]
     :rename {select-scheduled-products select-scheduled-videocs}])

  (def screen-name (->screen-name "0009"))
  (def products
    (select-scheduled-videocs {:db          (db-prod)
                               :limit       300
                               :screen-name screen-name}))

  (def titles (map :title products))

  (def names (into [] (map title->name titles)))


  )

(comment
  (def title "【配信限定特典映像付き】朝ドラ系現役アイドルT○kT○ker 西元めいさ 初体験で初絶頂 ＃初イキ ＃初巨根 ＃初3P ＃キャパオーバー ＃快感 ＃くびれボディ ＃ビクッビク【圧倒的4K映像でヌク！】")

  (->hashtags title)
  (title->trimed title)

  )
