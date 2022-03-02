((clojure-mode . (;; for M-x cider-jack-in
                  (cider-clojure-cli-aliases . "local")
                  ;; for M-x cider-ns-refresh (i.e. reloaded workflow)
                  (cider-ns-refresh-before-fn . "integrant.repl/suspend")
                  (cider-ns-refresh-after-fn  . "integrant.repl/resume"))))
