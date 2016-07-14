(ns airline.data.flights-test
  (:import [java.io StringReader StringWriter])
  (:require [clojure.test :refer :all]
            [airline.data.flights :refer :all]
            [airline.data.duplicates :as dup]
            [clj-time.core :as t]
            [clj-time.format :as f]))


(defn dt [yyyy mm dd hh mn tz]
  (t/from-time-zone
    (t/date-time yyyy mm dd hh mn)
    (t/time-zone-for-offset tz)))

;LHR,AKL,11422,16:15,05:25+2,NZ,1,744,Daily
(deftest test-combine-day-time-tz
  (testing "Combining a UTC local date and time"
    (is
      (= (dt 2014 10 4 16 15 (+ 0))
        (combine-day-time-tz
          (f/parse-local-date "2014-10-04")
          (f/parse-local "16:15")
          (t/time-zone-for-offset 0))))
    (is
      (= "2014-10-04T16:15:00Z"
        (to-iso-dt
          (combine-day-time-tz
            (f/parse-local-date "2014-10-04")
            (f/parse-local "16:15")
            (t/time-zone-for-offset 0))))))

  (testing "Combining an Aukland timezone"
    (is
      (= (dt 2014 10 6 5 25 (+ 12))
        (combine-day-time-tz
          (f/parse-local-date "2014-10-06")
          (f/parse-local "05:25")
          (t/time-zone-for-offset 12))))
    (is
      (= "2014-10-06T05:25:00+12:00"
        (to-iso-dt
          (combine-day-time-tz
            (f/parse-local-date "2014-10-06")
            (f/parse-local "05:25")
            (t/time-zone-for-offset 12)))))))

(defn fields [s]
  (clojure.string/split s #"[,\n]"))

(deftest test-critical-path
  (let [sw (StringWriter.)]
    (is
      (= (fields "2014-10-04,NZ1,LHR,2014-10-04T16:15:00Z,AKL,2014-10-06T05:25:00+12:00\n")
        (do
          (convert-rows
            (StringReader. "\nLHR,AKL,11422,16:15,05:25+2,NZ,1,744,Daily")
            sw
            (local-date-range (arg-start-date "2014-10-04") 1)
            {"LHR" (t/time-zone-for-offset 0)
             "AKL" (t/time-zone-for-offset 12)})
          (drop 1 (fields (.toString sw))))))))

(deftest test-filter-duplicates
  (testing "We ignore duplicate flight data in our source"
    (is (= ["AAL,CPH,148,17:35,18:20,SK,1218,CR9,6,0,N,\"Above 5/10 - 5/20\""]
          (remove #(dup/duplicate? (fields %))
            ["AAL,CPH,148,17:35,18:20,SK,1218,CR9,6,0,N,\"Above 5/10 - 5/20\""
             "AAL,CPH,148,17:35,18:20,SK,1218,CR9,6,0,N,\"Above 5/24 - 5/31\""])))))


(deftest test-timezones
  (let [sw (StringWriter.)]
    (is
      (= (fields "2014-10-05,AC8484,YEG,2014-10-05T13:25:00-06:00,YXE,2014-10-05T14:23:00-06:00\n")
        (do
          (convert-rows
            (StringReader. "\nYEG,YXE,298,13:25,14:23,AC,8484,CRJ,Daily,0,,")
            sw
            (local-date-range (arg-start-date "2014-10-05") 1)
            {"YEG" (t/time-zone-for-id "America/Edmonton")
             "YXE" (t/time-zone-for-id "America/Regina")})
          (drop 1 (fields (.toString sw))))))))

