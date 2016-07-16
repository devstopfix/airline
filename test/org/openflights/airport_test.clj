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

(testing "Type conversions"
  (deftest test-numeric-conversions
    (let [lhr (first (filter #(= "LHR" (:iata %)) (convert (airports))))]
      (is (= lhr {:iata "LHR",
                  :dst "E",
                  :offset 0.0,
                  :name "Heathrow",
                  :tz "Europe/London",
                  :icao "EGLL",
                  :city "London",
                  :latitude 51.4775,
                  :longitude -0.461389,
                  :altitude_feet 83.0,
                  :id "507",
                  :country "United Kingdom"})))))