(ns user
  (:require [aero.core :as ac]
            [clojure.java.io :as io]
            [monkey.ci.tester.network :as n]
            [monkey.oci.container-instance.core :as ci])
  (:import java.util.Base64))

(def config (ac/read-config (io/resource "config.edn")))

(defn- test-container [conf]
  {:image-url "fra.ocir.io/frjdhmocn5qi/network-tester:latest"
   :display-name "tester"
   :arguments ["/etc/network-tester/config.edn"]
   :volume-mounts
   [{:volume-name "config"
     :mount-path "/etc/network-tester"}]})

(defn- ->b64 [s]
  (.. (Base64/getEncoder)
      (encodeToString (.getBytes s))))

(defn- test-vol [conf]
  {:name "config"
   :volume-type "CONFIGFILE"
   :configs [{:file-name "config.edn"
              :data (->b64 (:config-file conf))}
             {:file-name "privkey"
              :data (->b64 (:private-key-file conf))}]})

(defn run-test
  "Runs the tester in an OCI container instance as configured"
  [conf]
  (let [ctx (ci/make-context (:oci conf))]
    (ci/create-container-instance
     ctx
     {:container-instance
      (-> (:ci conf)
          (merge
           {:shape "CI.Standard.A1.Flex"
            :shape-config
            {:ocpus 1
             :memory-in-g-bs 1}
            :container-restart-policy "NEVER"
            :display-name "network-tester"
            :containers [(test-container conf)]
            :volumes [(test-vol conf)]}))})))
