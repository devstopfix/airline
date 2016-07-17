(ns com.staralliance.flights
  (:use     [clojure-csv.core :only [parse-csv]]
            [org.iata.pnr :only [pnr-generator]])
  (:require [org.openflights.airport :as a]
            [clj-time.format :as f]
            [clj-time.core :as t]
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

(def gen-pid (gen/choose 9000 9999))

; Dates

(def days-ago (gen/choose 0 365))

(defn date-of-days-ago [days-ago t]
  "Return yyyy-mm-dd of n days ago from now"
  (let [d (-> (t/now) (t/minus (t/days days-ago)))
        s (f/unparse (f/formatters :year-month-day) d)]
    (format "%sT%sZ" s t)))

(def gen-flight
  (->>
    (flights)
    (into [] (filter-airports (european-airport-codes)))
    (map #(select-keys % [:source :leave :destination :arrive :carrier :flight]))
    (vec)
    (gen/elements)))

(def gen-trip (gen/tuple gen-pid pnr-generator gen-flight days-ago))

(defn cypher-create-trip [trip]
  (let [[pid pnr trip days-ago] trip]
    (format "MATCH (da:Airport {iata: \"%s\"}) MATCH (aa:Airport {iata: \"%s\"}) MERGE (p:Person {pid: %s}) CREATE (da)<-[:DEPART]-(f:Flight {no: \"%s\"})-[:ARRIVE]->(aa) CREATE (f)<-[:SEGMENT]-(t:Trip {pnr: \"%s\"})\nWITH 1 as dummy\n\n"
            (:source trip)
            (:destination trip)
            pid
            (str (:carrier trip) (:flight trip))
            pnr)))

; Example

;(->>
;  (flights)
;  (into [] (filter-airports (european-airport-codes)))
;  (map #(select-keys % [:source :leave :destination :arrive :carrier :flight]))
;  (take 10))

;(->>
;  (gen/sample gen-trip 100)
;  (map cypher-create-trip)
;  (clojure.string/join "\n")
;  (spit "/tmp/flights.cypher"))