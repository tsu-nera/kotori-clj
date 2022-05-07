(ns kotori.lib.provider.dmm.core)

(defn request-bulk
  [req-fn req-params]
  (->> req-params
       (pmap #(req-fn %)) ; まだ実行してない(lazy-seq)
       (doall) ; doallでマルチスレッド全発火.
       ))
