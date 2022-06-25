(ns kotori.domain.kotori.core
  (:require
   [clojure.spec.alpha :as s]
   [kotori.domain.dmm.genre.core :as genre]
   [kotori.domain.kotori.proxy :as proxy]
   [kotori.domain.kotori.strategy :as st]))

(def coll-name "kotoris")
(defn ->doc-path [user-id] (str coll-name "/" user-id))

(defrecord Cred [auth-token ct0 dmm-af-id])

;; TODO EDNファイルで定義してもいい.そしてRole Recordでもいい.
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

(def videoa-genres (floor-genres "videoa"))
(def videoc-genres (floor-genres "videoc"))
(def anime-genres (floor-genres "anime"))
(def doujin-genres (floor-genres "doujin"))

(defrecord Kotori
  [screen-name user-id code
   strategy cred proxy-info])

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
                   ::cred ::st/strategy]
          :opt-un [::proxy/info]))

(def guest-user "guest")

(defn code->genre-id [code]
  (let [[floor name] (get code-genre-map code)
        genre        (genre/make-genre floor)]
    (genre/name->id genre name)))

(defn config->cred-map [config]
  (select-keys config [:auth-token :ct0 :dmm-af-id]))

(defn kotori->af-id [kotori]
  (get-in kotori [:cred :dmm-af-id]))

;; Abstract Factory Pattern
(defn make-info [screen-name user-id code cred-map proxy-info]
  (let [cred       (s/conform ::cred (map->Cred cred-map))
        test-proxy (s/conform ::proxy/info (proxy/map->Info proxy-info))
        proxy-info (if-not (s/invalid? test-proxy) test-proxy {})
        genre-id   (code->genre-id code)
        strategy   (s/conform ::st/strategy (st/->Strategy genre-id))]
    (s/conform ::info (->Kotori screen-name user-id code strategy
                                cred proxy-info))))
