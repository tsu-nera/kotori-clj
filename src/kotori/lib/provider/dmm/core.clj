(ns kotori.lib.provider.dmm.core
  (:require
   [clojure.string :as str]))

(defn request-bulk
  [req-fn req-params]
  (->> req-params
       (pmap #(req-fn %)) ; まだ実行してない(lazy-seq)
       (doall) ; doallでマルチスレッド全発火.
       ))

(defn swap-af-id "
  APIで取得したaffiliateURLに個別サイトのaf-idを付与する.
  ついでにツールバーからのリンク作成へ形式を変更する."
  [af-id url]
  (let [partial-url (first (str/split url #"af_id="))]
    (str partial-url "af_id=" af-id "&ch=toolbar&ch_id=link")))

(comment
  (def af-url "https://al.dmm.co.jp/?lurl=https%3A%2F%2Fwww.dmm.co.jp%2Fdigital%2Fvideoa%2F-%2Fdetail%2F%3D%2Fcid%3Dmidv00119%2F&af_id=hogehoge-990&ch=api")
  (swap-af-id "hogehoge-001" af-url)
  )
