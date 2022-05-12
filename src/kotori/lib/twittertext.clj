(ns kotori.lib.twittertext
  " tweet-text-javaのClojure Wrapper.
    ref. https://github.com/twitter/twitter-text/tree/master/java"
  (:refer-clojure :exclude [count])
  (:import
   (com.twitter.twittertext
    Extractor
    TwitterTextParser)))

(defn- parse-tweet [text]
  (TwitterTextParser/parseTweet text))

(defn parse [text]
  (let [ret           (parse-tweet text)
        length        (.weightedLength ret)
        permillage    (.permillage ret)
        valid?        (.isValid ret)
        display-range (.displayTextRange ret)
        valid-range   (.validTextRange ret)]
    {:weighted-length     length
     :permillage          permillage
     :valid?              valid?
     :display-range-start (.start display-range)
     :display-range-end   (.end display-range)
     :valid-range-start   (.start valid-range)
     :valid-range-end     (.end valid-range)}))

(defn count "
  全角では140文字, 半角だと280文字.
  混在の場合のルールがよくわからないので結局ライブラリで計算がいい."
  [text]
  (let [ret   (parse text)
        start (:valid-range-start ret)
        end   (:valid-range-end ret)]
    (- end start)))

(defn ->hashtags [text]
  (.extractHashtags (Extractor.) text))

(comment
  ;;
  (def sample1 "test")
  (def sample2 "【配信限定特典映像付き】朝ドラ系現役アイドルT○kT○ker 西元めいさ 初体験で初絶頂 ＃初イキ ＃初巨根 ＃初3P ＃キャパオーバー ＃快感 ＃くびれボディ ＃ビクッビク【圧倒的4K映像でヌク！】")

  (parse sample2)
  (count sample2)
  ;;
  )
