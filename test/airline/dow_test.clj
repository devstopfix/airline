(ns airline.dow-test
  (:require [clojure.test :refer :all]
            [airline.dow :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :as ct :refer (defspec)]))

(deftest test-parse-every-day
  (let [every-day (list 1 2 3 4 5 6 7)]
    (testing "Every day of the week"
      (is (= every-day (parse "Daily")))
      (is (= every-day (parse "daily")))
      (is (= every-day (parse "1234567"))))))

(deftest test-parse-some-days
  (testing "Every some days of the week"
    (is (= (list 1)     (parse "1")))
    (is (= (list 1 2)   (parse "12")))
    (is (= (list 3 6 7) (parse "367")))
    (is (= (list 7)     (parse "7")))))

(deftest test-parse-weekdays
  (let [days (list 1 2 3 4 5)]
    (testing "Every week days"
      (is (= days (parse "12345")))
      (is (= days (parse "X67"))))))

(deftest test-parse-ignores-bad-days
  (testing "Bad days don't parse"
    (is (= nil (parse "18")))))

;
; Test properties
;
(defn valid-list? [dow]
  (or
    (nil? dow)
    (and
      (<= (count dow) 7)
      (every? #(and (>= % 1) (<= % 7)) dow))))

(defspec test-many-inputs 1e4
  (prop/for-all [s gen/string]
    (valid-list? (parse s))))
