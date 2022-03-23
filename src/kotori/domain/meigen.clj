(ns kotori.domain.meigen
  (:require
   [clojure.walk :refer [keywordize-keys]]
   [clojure.walk :refer [stringify-keys]]
   [firestore-clj.core :as f]))

(def meigens
  [{:content "苦悩を抜けて歓喜へ"
    :author  "ベートーベン"}
   {:content "志を立てて以って万事の源と為す"
    :author  "吉田松蔭"}
   {:content "高みに向かって努力を続けることは  決して無駄ではない"
    :author  "ニーチェ"}
   {:content "普通の奴らの上を行け"
    :author  "ポール・グレアム"}
   {:content
    "努力よりほかにわれわれの未来をよくするものはなく、また努力よりほかにわれわれの過去を美しくするものはないのである。"
    :author "幸田露伴"}
   {:content
    "努力だ。勉強だ。それが天才だ。誰よりも三倍、四倍、五倍勉強する者、それが天才だ。"
    :author "野口英世"}
   {:content "いちばんいけないのはじぶんなんかだめだと思いこむことだよ。" :author "野比のび太"}
   {:content
    "小さなことを重ねることが、とんでもないところに行くただひとつの道。"
    :author "イチロー"}
   {:content
    "私の人生を変えられるのは私だけ。誰も私のためにそんなことやってくれないわ。"
    :author "キャロル・バーネット"}
   {:content
    "天才とは99％の努力と1％のひらめきであるの努力と1％のひらめきである。"
    :author "トーマス・エジソン"}
   {:content
    "昨日から学び、今日を懸命に生き、明日への希望を持て。大切なことは問うことをやめないことだ。"
    :author "アインシュタイン"}
   {:content "苦しむこともまた才能の一つである。"
    :author  "ドフトエフスキー"}
   {:content
    "人生における大きな喜びは、「君にはできない」と世間が言うことをやってのけることである。"
    :author "ウォルター･バジョット"}
   {:content "失敗は成功の母だ"
    :author  "トーマス・エジソン"}
   {:content "苦しいから逃げるのではない。逃げるから苦しくなるのだ。"
    :author  "ウィリアム･ジェームズ"}
   {:content
    "平凡なことを毎日平凡な気持ちで実行することが、すなわち非凡なのである。"
    :author "アンドレ・ジッド"}
   {:content
    "努力は必ず報われる。もし報われない努力があるのならば、それはまだ努力と呼べない。"
    :author "王 貞治"}
   {:content "努力を癖にしてしまえば苦労せずに成功できるだろう。"
    :author  "流 音弥"}
   {:content "強い者が勝つのではない。勝った者が強いのだ。"
    :author  "フランツ・ベッケンバウワー"}
   {:content "努力する人は希望を語り、怠ける人は不満を語る。"
    :author  "井上 靖"}
   {:content
    "苦しみは人間を強くするか、それとも打ち砕くかである。その人が自分の内に持っている素質に応じてどちらかになるのである。"
    :author "カール・ヒルティ"}
   {:content
    "途中であきらめちゃいけない。途中であきらめてしまったら、得るものより失うものの方が、ずっと多くなってしまう。"
    :author "ルイ・アームストロング"}
   {:content
    "踏まれても叩かれても、努力さえしつづけていれば、必ずいつかは実を結ぶ。"
    :author "升田幸三"}
   {:content
    "石の上にも三年という。しかし、三年を一年で習得する努力を怠ってはならない。"
    :author "松下幸之助"}
   {:content "進まざる者は必ず退き、退かざる者は必ず進む。"
    :author  "福沢諭吉"}
   {:content
    "つらい道を避けないこと。自分の目指す場所にたどりつくためには進まなければ。"
    :author "キャサリン・アン・ポータ"}
   {:content
    "目標を達成するには、全力で取り組む以外に方法はない。そこに近道はない。"
    :author "マイケル・ジョーダン"}
   {:content "もうこれで満足だという時は、すなわち衰える時である。"
    :author  "渋沢栄一"}
   {:content
    "人間を賢くし人間を偉大にするものは、過去の経験ではなく、未来に対する期待である。なぜならば、期待をもつ人間は、何歳になっても勉強するからである。"
    :author "バーナード・ショー"}
   {:content
    "自分自身を最大限に利用しなさい。あなたにとって、あるのはそれだけなのですから。"
    :author "エマーソン"}
   {:content
    "人間は、目標を追い求める動物である。目標へ到達しようと努力することによってのみ、人生が意味あるものとなる。"
    :author "アリストテレス"}
   {:content
    "努力が効果をあらわすまでには時間がかかる。多くの人はそれまでに飽き、迷い、挫折する。"
    :author "ヘンリー・フォード"}
   {:content "自分に打ち勝つことが、最も偉大な勝利である。"
    :author  "プラトン"}
   {:content
    "重荷があるからこそ、人は努力するのである。重荷があるからこそ、大地にしっかりと足をつけて歩いていける。"
    :author "野村克也"}
   {:content
    "人間にとって最高の幸福は、一年の終わりにおける自己を、その一年の始めにおける遥かに良くなったと感ずることである。"
    :author "トルストイ"}
   {:content
    "生きている間は、なにごとも延期するな。なんじの実行また実行であれ。"
    :author "ゲーテ"}
   {:content "目的を忘れることは、愚かな人間にもっともありがちなことだ。"
    :author  "ニーチェ"}
   {:content "とじこめられている火が、いちばん強く燃えるものだ。"
    :author  "シェイクスピア"}
   {:content "満足は努力の中にあって、結果にあるものではない。"
    :author  "ガンジー"}
   {:content
    "普通の努力では、チャンスをチャンスと見極められない。熱心の上に熱心であることが見極める眼を開く。"
    :author "松下幸之助"}
   {:content "豊かさと平和は、臆病者をつくる。苦難こそ強さの母だ。"
    :author  "シェイクスピア"}
   {:content "逆境の中で咲く花は、どの花よりも貴重で美しい。"
    :author  "ウォルト・ディズニー"}
   {:content
    "喜びとは、勝利それ自体にではなく、途中の戦い、努力、苦闘の中にある。"
    :author "ガンジー"}
   {:content
    "昔を振り返るのはここでやめにしよう。大切なのは明日何が起きるかだ。"
    :author "スティーブ・ジョブス"}
   {:content
    "万策尽きたと思うな。自ら断崖絶壁の淵にたて。その時はじめて新たなる風は必ず吹く。"
    :author "松下幸之助"}
   {:content "人を信じよ、しかし、その百倍も自らを信じよ。"
    :author  "手塚治虫"}
   {:content "優れた人間は、どんなに不幸で苦しい境遇でも、黙って耐え忍ぶ。"
    :author  "ベートーベン"}
   {:content
    "人間の価値は、絶望的な敗北に直面して、いかにふるまうかにかかっている。"
    :author "ヘミングウェイ"}
   {:content
    "私は決して障害に屈しはしない。いかなる障害も、私の中に強い決意を生み出すまでだ。"
    :author "レオナルド・ダ・ヴィンチ"}
   {:content "必死に生きてこそ、その生涯は光を放つ。"
    :author  "織田信長"}
   {:content "世の人は我を何とも言わば言え、我が成すべきことは我のみぞ知る。"
    :author  "坂本龍馬"}
   {:content "自分をダメだと思えば、その時点から自分はダメになる。"
    :author  "モハメドアリ"}
   {:content "記憶に残る人生を歩め。"
    :author  "アヴィーチ"}
   {:content "決してギブアップしないヤツを打ち負かすことだけはできない。"
    :author  "ベーブ・ルース"}
   {:content "天才とは努力する凡才のことである。"
    :author  "アインシュタイン"}
   {:content "人事を尽くして天命を待つ"
    :author  ""}
   {:content
    "業なかばで倒れてもよい。そのときは、目標の方角にむかい、その姿勢で倒れよ。"
    :author "坂本龍馬"}
   {:content "真の楽しみは苦しみの中にこそある。"
    :author  "高杉晋作"}
   {:content
    "元気を出しなさい。今日の失敗ではなく、明日訪れるかもしれない成功について考えるのです。"
    :author "ヘレンケラー"}
   {:content
    "過去ばかり振り向いていたのではダメだ。自分がこれまで何をして、これまでに誰だったのかを受け止めた上で、それを捨てればいい。"
    :author "スティーブ・ジョブズ"}
   {:content
    "自分のことを、この世の誰とも比べてはいけない。それは自分自身を侮辱する行為だ。"
    :author "ビル・ゲイツ"}
   {:content "人間は、その人の思考の産物にすぎない。人は思っている通りになる。"
    :author  "ガンジー"}
   {:content "意志あるところに道は開ける。"
    :author  "リンカーン"}
   {:content
    "立派な人間は、天頂に星がきらめくときに、みずからの精神に火をともすのだ。"
    :author "ヴィクトル・ユーゴー"}
   {:content "人は強さに欠けているのではない。意志を欠いているのだ。"
    :author  "ヴィクトル・ユーゴー"}
   {:content
    "友よ、逆境にあっては、常にこう叫ばねばならない。「希望、希望、また希望」と。"
    :author "ヴィクトル・ユーゴー"}
   {:content
    "私は虚無と戦う生命なのだ。私は闇に燃える火だ。私は永遠に戦う自由な意思だ。私といっしょに戦え。そして燃えるがよい。"
    :author "ロマン・ロラン"}
   {:content
    "苦しめ、死ね。しかし、お前があらねばならぬものであれ。人間であれ。"
    :author "ロマン・ロラン"}
   {:content
    "それは淋しい空地において、暗い闘いを行う烈々たる行動の松明である。失敗も勝利も問題ではない、行動だ！行動し、戦闘することが、虚無に対する唯一の肯定なのだ。"
    :author "ロマン・ロラン"}
   {:content
    "何故生きるかを知っているものは、ほとんどあらゆる如何に生きるか、に耐えるのだ。"
    :author "ニーチェ"}
   {:content
    "意味は生存の歩調を定める。実存は、実存自体を超えた何物かへの超越性によって生きられない限り、躓く。"
    :author "ヴィクトール・フランクル"}
   {:content
    "地上に住むすべての人は、まず第一に生を愛さなければならないと思いますよ。"
    :author "ドストエフスキー"}])

(def coll-path "sources/source_0001/meigens")

(defn- get-coll-ids [coll]
  (let [docs (.listDocuments coll)]
    (map #(.getId %) docs)))

(defn pick-random [db]
  (let [coll     (f/coll db coll-path)
        coll-ids (get-coll-ids coll)
        id       (rand-nth coll-ids)]
    (-> coll
        (f/doc id)
        (.get)
        (deref)
        (.getData)
        (as-> x (into {} x))
        (keywordize-keys))))

(comment
  (require '[kotori.service.firebase :refer [get-db]])
  (pick-random (get-db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (def resp (-> coll
;;               (.get)))

;; (.getDocuments @resp)

;; (type coll)
;; (map #(.getId %) resp)

;; (comment

;;   (def docs (-> coll
;;                 (.get)
;;                 (deref)
;;                 (.getDocuments)
;;                 ))
;;   (def doc (first docs))

;;   (defn doc->map [doc]
;;     (-> doc
;;         (.getData)
;;         (as-> x (into {} x))
;;         (keywordize-keys)
;;         ))


;;   (def data (into [] (map doc->map docs)))
;;   )

;; => nil;; => nil
(comment
  ;; 今の実装だと, firestoreのIDがわからないため,
  ;; とりあえず全てのデータを取得してその中でランダムにindexを指定している.
  ;; firestoreのidはfirestoreの機能を利用して自動採番している.
  ;;
  ;; auto generated idは doc().idで参照するもので, documentのメンバとしては保持しないと.
  ;; https://www.reddit.com/r/Firebase/comments/dpnyln/how_to_create_an_autoid_inside_a_firestore/
  ;;
  ;; やりたいことはid指定でドキュメントを選択したいが, ローカルではidを保持したくない.
  ;; id listのみを取得は可能か？
  ;; まあもしくは初期化でいったん全部取得したらid listだけを状態で持っておくかな.
  ;; 決められたデータ・セットからランダムにpickするだけならそこまで大きなデータを扱うわけではないし.
  ;;
  ;; もしくは自分で採番するか. データの削除で欠番がめんどくさそうではある.
  ;; まあ時間かけてもしょうがないから簡単に片付けるか.
  ;;
  ;; お, .listDocumentsという関数でdocのリストが取得できた.
  ;; https://googleapis.dev/java/google-cloud-firestore/latest/com/google/cloud/firestore/CollectionReference.html
  ;;
  ;; やはりエラーメッセージとドキュメントをみるのが一番いいな.

  ;; (defn get-meigens []
  ;;   (let [query (.get @coll-meigens)]
  ;;     (->>
  ;;      (.getDocuments @query)
  ;;      (map #(.getData %)))))

  (defn get-coll-ids [coll]
    (let [docs (.listDocuments @coll)]
      (map #(.getId %) docs)))

  (reset! coll-ids (get-coll-ids))
  ;; 出来た,  pick-randomはindex指定に改造するぞ.

  (defn pick-random []
    (let [id (rand-nth @coll-ids)]
      (-> coll
          (f/doc id)
          (.get)
          (deref)
          (.getData))))

  ;; (def my-delay (delay (println "test")))
  ;; (force my-delay)
  ;; (def my-future (future (println "test")))
  ;; (pick-random)

  ;; (def docref (-> coll-meigens
  ;;                 (f/doc (rand-nth @coll-ids))
  ;;                 (.get)))


  ;; (def data (let [id (rand-nth @coll-ids)]
  ;;             (-> @coll-meigens
  ;;                 (f/doc id)
  ;;                 (.get)
  ;;                 (deref)
  ;;                 (.getData))))
  ;; (deref doc)
  ;; (.getData doc)
 ;;;
  )

(comment
  (defonce firestore (atom nil))
  firestore
  @firestore

  (def foo (atom 0))
  foo
  @foo
  (reset! foo 2)
  (swap! foo (fn [_] (+ 1 1)))

  (def bar (atom 1))
  (defonce bar (atom 0))
  bar
  @bar
  )

(comment
  firestore
  (require '[firestore-clj.core :as f])
  (f/coll @firestore fs-coll-path)
  )

;; (defn add-to-firestore [data]
;;   (let [fs-coll-meigens (.collection (get-fs) fs-coll-path)
;;         java-map        (stringify-keys data)]
;;     (.add fs-coll-meigens java-map)))

;; (map add-to-firestore meigens)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (get-fs)

;; (count meigens) ;; => 72
;; (get meigens 3) ;; => {:content "普通の奴らの上を行け", :author "ポール・グレアム"}

;; (def test-data  {:content "地上に住むすべての人は、まず第一に生を愛さなければならないと思いますよ。"
;;                  :author  "ドストエフスキー"})

;; (add-to-firestore test-data)

;; (def java-map (java.util.HashMap. test-data))
;; (def clj-map  (into {} java-map))
;; (stringify-keys test-data)

;; (def docRef (-> db
;;                 (.collection "users")
;;                 (.document "alovlance2")))
;; (def data {"first" "Ada"
;;            "last"  "Lovelance"
;;            "born"  1815})
;; (def result (. docRef set data))