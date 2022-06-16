(ns kotori.domain.kotori.core
  (:require
   [clojure.spec.alpha :as s]
   [kotori.domain.dmm.genre.core :as genre]))

(def coll-name "kotoris")
(defn ->doc-path [user-id] (str coll-name "/" user-id))

(defrecord Cred [auth-token ct0 dmm-af-id])
(defrecord Proxy [proxy-host proxy-port proxy-user proxy-pass])

;; TODO EDNファイルで定義してもいい.そしてRole Recordでもいい.
(def code-genre-map
  {"0001" ["videoa" nil]
   "0002" ["videoa" "痴女"] ;1031
   "0007" ["videoa" "超乳"] ;6149
   "0009" ["videoa" "素人"] ;4024
   "0010" ["videoa" "ぽっちゃり"] ;2007
   "0011" ["videoa" "熟女"] ;1014
   "0019" ["videoa" "ギャル"] ;1034
   "0020" ["videoa" "イラマチオ"];5068
   "0024" ["anime" nil]
   "0025" ["videoa" "M男"]
   "0027" ["videoc" nil]
   "0028" ["videoa" "VR専用"] ;6793
   "0040" ["videoc" "ぽっちゃり"] ; 8510. 2007ではない.
   })

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

(defrecord Info
  [screen-name user-id code genre-id
   ^Cred cred ^Proxy proxy])

(s/def ::auth-token string?)
(s/def ::ct0 string?)
(s/def ::dmm-af-id (s/nilable string?))
(s/def ::cred
  (s/keys :req-un [::auth-token ::ct0]
          :opt-un [::dmm-af-id]))

(s/def ::proxy-host string?)
(s/def ::proxy-port int?)
(s/def ::proxy-user string?)
(s/def ::proxy-pass string?)
(s/def ::proxy
  (s/keys :opt-un [::proxy-host ::proxy-port ::proxy-user ::proxy-pass]))

(s/def ::screen-name string?)
(s/def ::user-id string?)
(s/def ::code string?)
(s/def ::genre-id (s/nilable int?))
(s/def ::info
  (s/keys :req-un [::screen-name ::user-id ::code ::cred]
          :opt-un [::proxy ::genre-id]))

(def guest-user "guest")

(defn code->genre-id [code]
  (let [[floor name] (get code-genre-map code)
        genre        (genre/make-genre floor)]
    (genre/name->id genre name)))

(defn config->cred-map [config]
  (select-keys config [:auth-token :ct0 :dmm-af-id]))

(defn info->af-id [info]
  (get-in info [:cred :dmm-af-id]))

;; Abstract Factory Pattern
(defn make-info [screen-name user-id code cred-map proxy-map]
  (let [cred       (s/conform ::cred (map->Cred cred-map))
        test-proxy (s/conform ::proxy (map->Proxy proxy-map))
        proxy      (if-not (s/invalid? test-proxy) test-proxy {})
        genre-id   (code->genre-id code)]
    (s/conform ::info (->Info screen-name user-id code genre-id
                              cred proxy))))
