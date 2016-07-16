(ns org.iata.pnr-test
  (:require [clojure.test :refer :all]
            [org.iata.pnr :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :as ct :refer (defspec)]))

; Helper

(def string-alpha-numeric-uppercase
  (gen/fmap
    #(.toUpperCase %)
    gen/string-alpha-numeric))

;
; Test we can validate strings containing PNR numbers
;

(testing "Valid PNR numbers"
  (deftest test-valid-pnr
    (is (valid-pnr? "2ID6EW")))

  (defspec test-pnr-numbers-are-length-6
           (prop/for-all [s string-alpha-numeric-uppercase]
                         (is (=
                               (= 6 (count s))
                               (valid-pnr? s))))))

(testing "Invalid PNRs"
  (deftest test-empty-str-is-invalid-pnr
    (is (not (valid-pnr? ""))))

  (defspec test-strings-are-not-usally-valid-pnr
           (prop/for-all [s gen/string]
                         (is (not (valid-pnr? s))))))


(testing "We can generate valid PNR numbers"
  (defspec test-pnr-generator
           (prop/for-all [pnr pnr-generator]
                         (is (valid-pnr? pnr)))))