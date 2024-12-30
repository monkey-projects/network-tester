(ns monkey.ci.tester.network
  "Tests various network connections: some should be accessible, others cannot be"
  (:gen-class)
  (:require [aero.core :as aero]
            [aleph.tcp :as tcp]
            [clojure.tools.logging :as log]
            [manifold
             [deferred :as md]
             [stream :as ms]]
            [monkey.aero]
            [monkey.oci.os.core :as os]))

(def check-timeout 1000)

(defn check! [[host port]]
  ;; TODO Protocol-specific tests
  (log/infof "Checking: %s:%d" host port)
  (-> (md/chain
       (tcp/client {:host host
                    :port port})
       ms/close!
       (constantly :connected))
      (md/timeout! check-timeout :failed)))

(defn publish-results [{:keys [oci]} res]
  (let [ctx (os/make-client oci)
        ns @(os/get-namespace ctx)
        t (System/currentTimeMillis)
        dest (str (:prefix oci) "check-" t ".edn")]
    (log/info "Publishing results to" dest)
    @(os/put-object ctx {:ns ns
                         :bucket-name (:bucket-name oci)
                         :object-name dest
                         :contents (pr-str res)
                         :martian.core/request {:headers
                                                {"content-type" "application/edn"}}})
    res))

(defn run-checks [{{:keys [allowed forbidden]} :services :as conf}]
  (letfn [(do-check [[c expected]]
            (md/chain
             (check! c)
             (fn [r]
               {:config c
                :result [expected r]})))
          (print-results [r]
            (log/info "Ran" (count r) "checks")
            (let [failed (filter (comp (partial apply not=) :result) r)]
              (when (not-empty failed)
                (log/infof "%d checks failed:" (count failed))
                (doseq [{:keys [config] [exp act] :result} failed]
                  (log/info "Service:" config ", expected:" exp ", actual:" act))))
            r)]
    (->> (concat (map #(vector % :connected) allowed)
                 (map #(vector % :failed) forbidden))
         ;; Run checks in parallel
         (map do-check)
         (apply md/zip)
         (deref)
         (print-results)
         (publish-results conf)
         (every? (comp true? last)))))

(defn -main [& args]
  (let [conf (aero/read-config (first args))]
    (when (false? (run-checks conf))
      (System/exit 1))))
