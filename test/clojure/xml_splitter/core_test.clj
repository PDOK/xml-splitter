(ns xml-splitter.core-test
  (:require [xml-splitter.core :as core]
            [clojure.test :refer :all]))

(deftest start-end-tag
  (testing "Start and end-tag based on splitter")
  (is (= ["<ns1:foo>" "</ns1:foo>"] (core/start-and-end-tag "ns1:foo"))))
