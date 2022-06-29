(ns kotori.domain.kotori.core
  (:require
   [clojure.spec.alpha :as s]
   [kotori.domain.dmm.genre.core :as genre]
   [kotori.domain.kotori.proxy :as proxy]
   [kotori.domain.kotori.strategy :as strategy]))

(def coll-name "kotoris")
(defn ->doc-path [user-id] (str coll-name "/" user-id))

(defrecord Cred [auth-token ct0 dmm-af-id])

(defrecord Kotori
  [screen-name user-id code
   strategy cred proxy])

(s/def ::auth-token string?)
(s/def ::ct0 string?)
(s/def ::dmm-af-id (s/nilable string?))
(s/def ::cred
  (s/keys :req-un [::auth-token ::ct0]
          :opt-un [::dmm-af-id]))

(s/def ::screen-name string?)
(s/def ::user-id string?)
(s/def ::code string?)

(s/def ::info
  (s/keys :req-un [::screen-name ::user-id ::code
                   ::cred
                   ::strategy/strategy]
          :opt-un [::proxy/proxy]))

(def guest-user "guest")

(defn ->af-id [kotori]
  (get-in kotori [:cred :dmm-af-id]))

(defn ->coll-path [kotori]
  (get-in kotori [:strategy :coll-path]))

(defn ->genre-id [kotori]
  (get-in kotori [:strategy :genre-id]))

(defn config->cred-map [config]
  (select-keys config [:auth-token :ct0 :dmm-af-id]))

;; Abstract Factory Pattern
(defn create [screen-name user-id code
              cred-map
              strategy
              proxy]
  (let [cred     (s/conform ::cred (map->Cred cred-map))
        proxy    (proxy/create proxy)
        strategy (strategy/create strategy)]
    (s/conform ::info (->Kotori screen-name user-id code strategy
                                cred proxy))))

(defn config->kotori [{:keys
                       [screen-name user-id code proxy strategy]
                       :as m}]
  (let [cred-map (config->cred-map m)]
    (create screen-name user-id code
            cred-map
            strategy
            proxy)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;; TODO リファクタリングによって不要に鳴ったコード.
  ;; 本当に不要か考慮したら削除.
  ;; できればこのmapは残しつつ integrantでname-idのresolveをしたい.

  (def code-genre-map
    {"0001" ["videoa" nil]
     "0002" ["videoa" "痴女"] ;1031
     "0007" ["videoa" "超乳"] ;6149
     "0009" ["videoa" "ハメ撮り"] ;4024
     "0010" ["videoa" "ぽっちゃり"] ;2007
     "0011" ["videoa" "熟女"] ;1014
     "0019" ["videoa" "ギャル"] ;1034
     "0020" ["videoa" "イラマチオ"];5068
     "0024" ["anime" nil]
     "0025" ["videoa" "M男"] ;5005
     "0026" ["doujin" "女性向け"]
     "0027" ["videoc" nil]
     "0028" ["videoa" "VR専用"] ;6793
     "0029" ["doujin" "男性向け"]
     "0034" ["doujin" "女性向け"]
     "0040" ["videoc" "ぽっちゃり"] ; 8510. 2007ではない.
     "0041" ["videoa" "母乳"]})

  (defn floor-genres [name]
    (let [genre (genre/make-genre name)]
      (->>
       (for [[k v] (vals code-genre-map)
             :when (= k name)]
         (genre/name->id genre v))
       (filter some?)
       (into #{}))))

  (defn code->genre-id [code]
    (let [[floor name] (get code-genre-map code)
          genre        (genre/make-genre floor)]
      (genre/name->id genre name)))

  (def videoa-genres (floor-genres "videoa"))
  (def videoc-genres (floor-genres "videoc"))
  (def anime-genres (floor-genres "anime"))
  (def doujin-genres (floor-genres "doujin"))
  )
