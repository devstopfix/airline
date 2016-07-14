(ns airline.data.flights
  (:import [java.io File Writer])
  (:use [clojure-csv.core :only [parse-csv write-csv]])
  (:require [airline.dow :as dow]
            [airline.data.airports :as ap]
            [airline.data.duplicates :as dup]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [devstopfix.auid :as auid])
  (:gen-class))

(def day-fmt (f/formatters :year-month-day))
(def time-fmt (f/formatters :hour-minute))
(def iso-dt-fmt (.withOffsetParsed
                  (f/formatters :date-time-no-ms)))

(defn to-iso-dt [d]
  "Convert date-time to ISO string with timezone."
  (f/unparse iso-dt-fmt d))

(defn print-err [msg]
  "Print error to STDOUT."
  (binding [*out* *err*]
    (println msg)))

(defn write-rows [^Writer out rows]
  "Write out a list of rows, all fields
   are expected to be Strings."
  (.write out
    (write-csv
      (doall rows))))

(defn prepare-str [row]
  "Convert Dates to Strings ready for output."
  (let [[day dept flight scheduled dest arrive] row]
    [(f/unparse-local-date day-fmt day)
     flight
     dept
     (to-iso-dt scheduled)
     dest
     (to-iso-dt arrive)]))

; We want to show the local time at the airport, not UTC
; from-time-zone uses withZoneRetainFields
; https://github.com/clj-time/clj-time/blob/master/src/clj_time/core.clj#L368
; http://joda-time.sourceforge.net/api-release/org/joda/time/DateTime.html#withZoneRetainFields(org.joda.time.DateTimeZone)
(defn combine-day-time-tz [local-date local-time tz]
  (t/from-time-zone
    (t/date-time
      (.getYear local-date)
      (.getMonthOfYear local-date)
      (.getDayOfMonth local-date)
      (.getHourOfDay local-time)
      (.getMinuteOfHour local-time)
      (.getSecondOfMinute local-time))
    tz))

(defn arrival-time [day arrive tz]
  "Arrive time may be on same or next day as departure date,
   indicated with a '+1' or '-1' suffix on the time."
  (if-let [arrive (re-matches #"(\d\d:\d\d)([+-])(\d)" arrive)]
    (let [[_ at sign days] arrive]
      (if (= "+" sign)
        (combine-day-time-tz
          (t/plus day (t/days (Integer/parseInt days)))
          (f/parse-local time-fmt at)
          tz)
        (combine-day-time-tz
          (t/minus day (t/days (Integer/parseInt days)))
          (f/parse-local time-fmt at)
          tz)))
    (combine-day-time-tz
      day
      (f/parse-local time-fmt arrive)
      tz)))

(defn departure-time [day leave tz]
  (combine-day-time-tz
    day
    (f/parse-local time-fmt leave)
    tz))

(defn flight-details [airports row]
  "Generate the flight details for the row."
  (let [[dept dest leave arrive flight day] row]
    [day
     dept
     flight
     (departure-time day leave (get airports dept))
     dest
     (arrival-time day arrive (get airports dest))]))

(defn arrive-after-depart? [row ]
  (let [[_ _ _ leave _ arrive] row]
    (if (t/after? arrive leave)
      true
      (do
        (print-err (str "Arrives before depart:" row))
        false))))

  (defn filter-days-of-week [date-seq dow]
  "Filter a sequence of dates by their day-of-week.
   dow must respond to contains? (e.g. a Set) and
   be a set of days where Monday is 1 and Sunday is 7."
  (filter
    #(contains? dow (t/day-of-week %))
    date-seq))

(defn expand-row [date-range row]
  "Take a single row where the last field is a set of the days
   of the week on which this flight runs, and return a seq of
   multiple rows for each day that this flight flies."
  (let [[_ _ _ _ _ days] row
        days-filter (into #{} days)]
    (for [d (filter-days-of-week date-range days-filter)]
      (assoc row 5 d))))

(defn extract-row [row]
  "Extract relevant fields from row"
  (let [[source destination miles leave arrive
         carrier flight plane days-str & args] row
        days (dow/parse days-str)]
    (when days
      [source
       destination
       leave
       arrive
       (str carrier flight)
       days])))

(defn prepend-ids [next-id row]
  "Prepend a surrogate key for the row."
  (cons (format "%d" (next-id)) row))

(defn convert-rows [in ^Writer out date-range airports]
  "Convert each row from input stream."
  (let [next-id (auid/next-id-fn 0)]
    (doseq [line (drop 1 (parse-csv in))]
      (->> line
        (extract-row)
        (expand-row date-range)
        (remove dup/duplicate?)
        (map (partial flight-details airports))
        (filter arrive-after-depart?)
        (map prepare-str)
        (map (partial prepend-ids next-id))
        (write-rows out)))))

(defn local-date-range [start-date ^Long n]
  (map
    #(t/plus start-date (t/days %))
    (range n)))

(defn run [in start-date day-count airports]
  "Read the input file, convert and write to STDOUT."
  (with-open [rdr (clojure.java.io/reader in)]
    (convert-rows
      rdr
      *out*
      (local-date-range start-date day-count)
      airports)))

(defn fail [msg]
  "Stop the program."
  (do
    (binding [*out* *err*]
      (println msg))
    (System/exit 1)))

(defn arg-start-date [start-date]
  (f/parse-local
    (f/formatters :year-month-day)
    start-date))

(defn arg-days [days]
  (Long/parseLong days))

(use 'clojure.pprint)

; lein run resources/airports.dat resources/StarAlliance200905.csv 2014-10-04 30
(defn -main [airports in start-date days & args]
  (let [in (File. in)
        airports (File. airports)]
    (if (.exists in)
      (run
        in
        (arg-start-date start-date)
        (arg-days days)
        (ap/iata-to-tz airports))
      (fail
        (format "Input file not found '%s'" in)))))
