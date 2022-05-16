(ns kotori.domain.dmm.genre.interface)

(defmulti id->name (fn [floor id] floor))
