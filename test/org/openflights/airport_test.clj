(ns org.openflights.airport-test
  (:require [clojure.test :refer :all]
            [org.openflights.airport :refer :all]))

(testing "We can read airport data"
  (deftest test-read-first-row
    (let [airport (first (airports))]
      (is (= "1"   (:id airport)))
      (is (= "GKA" (:iata airport)))))
  (deftest test-row-count
    (let [last-airport (last (airports))]
      (is (= "9541" (:id last-airport))))))

(testing "Airport reducers"
  (deftest test-count-european-airports
    (is (= 1852 (->> (airports)
                    (into [] filter-european-tz)
                     (count)))))
  (deftest test-count-european-airports
    (is (= 1046 (->> (airports)
                     (into [] only-european-iata-airports)
                     (count))))))
