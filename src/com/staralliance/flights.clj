(ns com.staralliance.flights
  (:use     [clojure-csv.core :only [parse-csv]]
            [org.iata.pnr :only [pnr-generator]])
  (:require [org.openflights.airport :as a]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.test.check.generators :as gen]))

(def timetable "/usr/local/share/staralliance.com/StarAlliance200905.csv")

(defn header-fields [row]
  (->> row
       (map #(keyword (clojure.string/lower-case %)))
       (vec)))

(defn flights []
  "Return a seq of maps of flights"
  (lazy-seq
    (with-open [rdr (clojure.java.io/reader timetable)]
      (let [rows (parse-csv rdr :delimiter \,)
            header (header-fields (first rows))]
        (->> rows
             (rest)
             (map #(zipmap header %))
             doall)))))

; Filter flights by airport

(defn filter-airport-depart
  [airports] (filter #(contains? airports (:source %))))

(defn filter-airport-arrive
  [airports] (filter #(contains? airports (:destination %))))

(defn filter-airports [airports]
  (comp (filter-airport-depart airports) (filter-airport-arrive airports)))

(defn european-airport-codes []
  "Set of IATA codes for European airports"
  (->>
    (a/airports)
    (a/convert)
    (into [] a/only-european-iata-airports)
    (map :iata)
    set))

; PID

(def gen-pid (gen/choose 9000 9099))

; Dates

(def days-ago (gen/choose 0 365))

(defn date-of-days-ago [days-ago t now]
  "Return yyyy-mm-dd of n days ago from now"
  (let [d (-> now (t/minus (t/days days-ago)))
        s (f/unparse (f/formatters :year-month-day) d)]
    (format "%sT%s:00Z" s t)))

(defn iso-to-long [dt]
  (->>
    dt
    (f/parse (f/formatters :date-time-no-ms))
    (c/to-long)))

(def gen-flight
  (->>
    (flights)
    (into [] (filter-airports (european-airport-codes)))
    (map #(select-keys % [:source :leave :destination :arrive :carrier :flight]))
    (vec)
    (gen/elements)))

(def gen-trip (gen/tuple gen-pid pnr-generator gen-flight days-ago))

(defn timestamp-trip
  ([trip days-ago] (timestamp-trip trip days-ago (clj-time.core/now)))
  ([trip days-ago now]
    (let [depart-at (date-of-days-ago days-ago (:leave trip)  now)
          arrive-at (date-of-days-ago days-ago (:arrive trip) now)]
      (-> trip
          (assoc :leave depart-at)
          (assoc :arrive arrive-at)))))

(defn cypher-create-trip
  ([trip] (cypher-create-trip trip (clj-time.core/now)))
  ([trip now] (let [[pid pnr trip days-ago] trip
         trip (timestamp-trip trip days-ago now)
         arrives_at (iso-to-long (:arrive trip))
         departs_at (iso-to-long (:leave trip))]
     (->>
       [(format "MATCH (da:Airport {iata: \"%s\"})" (:source trip))
        (format "MATCH (aa:Airport {iata: \"%s\"})" (:destination trip))
        (format "MERGE (p:Person {pid: %s})" pid)
        (format "CREATE (da)<-[:DEPARTS]-(f:Flight {no: \"%s\", departs_at: \"%s\", arrives_at: \"%s\"})-[:ARRIVES]->(aa)"
                (str (:carrier trip) (:flight trip))
                (:leave trip)
                (:arrive trip))
        (format "CREATE (f)<-[:SEGMENT]-(t:Trip {pnr: \"%s\", name: \"%s\"})" pnr pnr)
        (format "CREATE (p)-[:TRAVELS]->(t)")
        (format "CREATE (p)-[:EXPECTED_AT {at: %d}]->(aa)" arrives_at)
        (format "CREATE (p)-[:TRANSMITS   {at: %d}]->(:Point:Location {latitude: da.latitude, longitude: da.longitude})" departs_at)]
       (clojure.string/join " \n")))))

; Example

;(->>
;  (flights)
;  (into [] (filter-airports (european-airport-codes)))
;  (map #(select-keys % [:source :leave :destination :arrive :carrier :flight]))
;  (take 10))


;(use 'com.staralliance.flights)
;
;(->>
;  (gen/sample gen-trip 100)
;  (map cypher-create-trip)
;  (clojure.string/join "\n\nWITH 1 as dummy\n")
;  (spit "/tmp/flights.cypher"))