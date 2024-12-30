(ns build
  (:require [monkey.ci.build.v2 :as m]
            [monkey.ci.plugin.kaniko :as pk]))

(def uberjar-artifact
  (m/artifact "uberjar"
              "target/network-tester.jar"))

(def uberjar
  (-> (m/container-job "uberjar")
      (m/image "docker.io/clojure:tools-deps-bullseye-slim")
      (m/script ["clj -X:uberjar"])
      (m/save-artifacts uberjar-artifact)))

(def image
  (pk/multi-platform-image-job
   {:target-img "fra.ocir.io/frjdhmocn5qi/network-tester:latest"
    :archs [:arm :amd]
    :container-opts
    {:dependencies ["uberjar"]
     :restore-artifacts [uberjar-artifact]}}))

[uberjar
 image]
