(ns org.openflights.airport
  (:use     [clojure-csv.core :only [parse-csv]]))

; See README and http://openflights.org/data.html
(def airports-dat "/usr/local/share/openflights.org/airports.dat")

(def airports-dat-columns [:id :name :city :country :iata :icao :latitude :longitude :altitude_feet :offset :dst :tz])

(defn- row-to-map [row]
  "Convert a vector of strings to a map"
  (zipmap airports-dat-columns row))

(defn airports []
  "Return a seq of maps of airports"
  (lazy-seq
    (with-open [rdr (clojure.java.io/reader airports-dat)]
     (->>
       (parse-csv rdr :delimiter \,)
       (map row-to-map)
       doall))))

(def remove-without-iata
  (remove #(.isEmpty (:iata %))))

(def filter-european-tz
  (filter #(= "E" (:dst %))))

(def only-european-iata-airports (comp remove-without-iata filter-european-tz))

(defn str-to-double [^String s]
  (Double/parseDouble s))

(defn- convert-row [row]
  (-> row
      (update :altitude_feet str-to-double)
      (update :offset str-to-double)
      (update :latitude str-to-double)
      (update :longitude str-to-double)))

(defn convert
  "Convert numeric strings to Doubles"
  ([] (map convert-row))
  ([rows] (map convert-row rows)))
