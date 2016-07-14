(defproject airline.data.flights "0.5.0"
  :description "Generate a dataset of flights from an airline timetable."
  :url "https://github.com/devstopfix/airline/airline.data.flights"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main airline.data.flights
  :aot [airline.data.flights]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-time "0.9.0"]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [org.clojure/core.cache "0.6.4"]
                 [devstopfix.auid "0.3.0"]
                 [org.clojure/test.check "0.9.0"]])
