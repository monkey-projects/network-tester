(ns build-test
  (:require [clojure.test :refer [deftest testing is]]
            [build :as sut]
            [monkey.ci.build.v2 :as m]
            [monkey.ci.test :as t]))

(deftest uberjar
  (testing "is container job"
    (is (m/container-job? sut/uberjar))))

(deftest image
  (t/with-build-params {}
    (testing "produces multiple jobs"
      (is (= 3 (count (sut/image t/test-ctx)))))))
