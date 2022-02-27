(ns build
  (:require
   [clojure.tools.build.api :as b]))

(def lib 'tsu-nera/kotori)
(def version (format "0.1.%s" (b/git-count-revs nil)))

(def target-dir "target")
(def class-dir (str target-dir "/" "classes"))
(def artifacts-dir (str target-dir "/" "artifacts"))
(def jar-file (format "%s/%s-%s-application.jar" artifacts-dir (name lib) version))
(def uber-file (format "%s/application.jar" artifacts-dir))

(def src ["src"])
(def basis (b/create-basis {:project "deps.edn"}))

;; clj -T:build clean
(defn clean [_]
  ;; "delete the build target directory"
  (println (str "Cleaning... " target-dir))
  (b/delete {:path "target"}))


;; clj -T:build jar
(defn jar [_]
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  src})
  (b/copy-dir {:src-dirs   src
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file  jar-file}))

;; clj -T:build uber
(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs   src
               :target-dir class-dir})
  (b/compile-clj {:basis     basis
                  :src-dirs  src
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     basis
           }))

;; 1. Compile the code and generate a pom.xml
(defn prep [_]
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  src})
  (b/compile-clj {:basis     basis
                  :src-dirs  src
                  :class-dir class-dir}))

;; 2. Containerize with Jib and upload the container to registry
;; (defn jib [_]
;; nil)

;; (defn jib [_]
;;   (.containerize
;;       (-> (Jib/from "gcr.io/distroless/java:11")
;;           ;; (.addLayer [(Paths/get uber-path (into-array String[]))] (AbsoluteUnixPath/get "/"))
;;           ;; (.setProgramArguments [(format "/%s" uber-file)])
;;           (.addExposedPort (Port/tcp 3000))
;;           )
;; Jib.from("busybox")
;; .addLayer(Arrays.asList(Paths.get("helloworld.sh")), AbsoluteUnixPath.get("/"))
;; .setEntrypoint("sh", "/helloworld.sh")
;; .containerize(
;;           Containerizer.to(RegistryImage.named("gcr.io/my-project/hello-from-jib")
;;                                             .addCredential("myusername", "mypassword")));
;; )
