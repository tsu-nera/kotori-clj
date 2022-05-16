(ns kotori.lib.time
  (:refer-clojure :exclude [range iterate format max min])
  (:require
   [java-time :as t]))

;; Locale.JAPAN -----> ja_JP
;; Locale.JAPANESE	--> ja
;; (def locale-jp "ja_JP")
(def locale-jst java.util.Locale/JAPAN)
(def tz-jst-str "Asia/Tokyo")
(def format-dmm "yyyy-MM-dd HH:mm:ss")

;; python codeとの互換性を考慮してこのformatにしておく
(def format-log "yyyy-MM-dd HH:mm:ss,SSS")

(defn before?
  "t2はt1よりも前か？"
  [t1 t2]
  (t/before? t1 t2))

(defn after?
  "t2はt1よりもあとか？"
  [t1 t2]
  (t/after? t1 t2))

(def tz-jst
  (java.util.TimeZone/getTimeZone tz-jst-str))

(defn ->tz-jst
  "TimezoneにJSTを設定."
  [timestamp]
  (t/zoned-date-time timestamp tz-jst-str))

(defn str->java-time [format timestamp]
  (->tz-jst
   (t/local-date-time format timestamp)))

(defn now
  "現雑時刻(日本標準時)のjava timeを返す"
  []
  (->tz-jst
   ;; (t/local-date-time)
   (t/zoned-date-time)))

(defn date->days-ago
  ([x]
   (date->days-ago x (now)))
  ([x date]
   (->tz-jst (t/minus date (t/days x)))))

(defn date->days-later
  ([x]
   (date->days-later x (now)))
  ([x date]
   (->tz-jst (t/plus date (t/days x)))))

(defn date->weeks-ago
  ([x]
   (date->weeks-ago x (now)))
  ([x date]
   (->tz-jst (t/minus date (t/weeks x)))))

(defn date->weeks-later
  ([x]
   (date->weeks-later x (now)))
  ([x date]
   (->tz-jst (t/plus date (t/weeks x)))))

(defn parse-timestamp-sdf [format ^String timestamp]
  (let [locale java.util.Locale/US
        sdf    (java.text.SimpleDateFormat.  format locale)]
    (.setTimeZone sdf tz-jst)
    (.parse sdf timestamp)))

(defn parse-dmm-timestamp [str]
  (str->java-time format-dmm str))

(defn ->fs-timestamp "
  java.time型のタイムスタンプをFirestoreに 送信すると
  オブジェクトとして保存される.
  java.util.Dateに変換して送信するとタイムスタンプとして扱われる."
  [timestamp]
  (t/java-date timestamp))

(defn fs-now []
  (t/java-date (now)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(now)
#_(date->weeks-ago 4)

(comment
  (def dmm-timestamp "2022-02-18 10:00:57")
  (parse-dmm-timestamp dmm-timestamp)
  )
