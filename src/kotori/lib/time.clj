(ns kotori.lib.time
  (:refer-clojure :exclude [range iterate format max min])
  (:require
   [java-time :as t]))

(def tz-jst "Asia/Tokyo")
(def format-twitter "EEE MMM dd HH:mm:ss Z yyyy")
(def format-dmm "yyyy-MM-dd HH:mm:ss")

(defn before? [t1 t2]
  (t/before? t1 t2))

(defn after? [t1 t2]
  (t/after? t1 t2))

(defn ->tz-jst
  "TimezoneにJSTを設定."
  [timestamp]
  (t/zoned-date-time timestamp tz-jst))

(defn str->java-time [format timestamp]
  (->tz-jst (t/local-date-time format timestamp)))

(defn now []
  "現雑時刻(日本標準時)のjava timeを返す"
  (->tz-jst (t/local-date-time)))

(defn weeks-ago
  "現雑時刻(日本標準時)のX週間前を返す"
  [x]
  (->tz-jst (t/minus (now) (t/weeks x))))

(defn parse-twitter-timestamp
  "月と曜日が英語表記の場合のparseがうまくいかないので
  とりあえず実績のあるSimpleDateFormatで対処することにした."
  [str]
  (let [locale java.util.Locale/US
        sdf    (java.text.SimpleDateFormat. format-twitter locale)]
    (.setTimeZone sdf (java.util.TimeZone/getTimeZone tz-jst))
    (.parse sdf str)))

(defn parse-dmm-timestamp [str]
  (str->java-time format-dmm str))

(defn ->fs-timestamp "
  java.time型のタイムスタンプをFirestoreに 送信するとオブジェクトとして保存される.
  java.util.Dateに変換して送信するとタイムスタンプとして扱われる."
  [timestamp]
  (t/java-date timestamp))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(now)
#_(weeks-ago 4)

(comment
  (def dmm-timestamp "2022-02-18 10:00:57")
  (parse-dmm-timestamp dmm-timestamp)

  (def twitter-timestamp "Sat Mar 26 02:15:15 +0000 2022")
  (parse-twitter-timestamp twitter-timestamp)

  (defn parse-timestamp-sdf [format timestamp]
    (let [locale java.util.Locale/US
          sdf    (java.text.SimpleDateFormat. format locale)]
      (.setTimeZone sdf (java.util.TimeZone/getTimeZone "Asia/Tokyo"))
      (.parse sdf timestamp)))

  (defn parse-twitter-timestamp-sdf [timestamp]
    (parse-timestamp-sdf format-twitter timestamp))

  (parse-twitter-timestamp-sdf twitter-timestamp)
  )

(comment
  (t/minus (now) (t/weeks 3))
  )
