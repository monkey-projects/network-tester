(ns build-test
  (:require [clojure.test :refer [deftest testing is]]
            [build :as sut]))

(deftest uberjar
  (testing "is container job"
    (is (m/container-job? sut/uberjar))))
