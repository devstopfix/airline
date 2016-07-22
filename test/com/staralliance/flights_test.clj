(ns com.staralliance.flights-test
  (:require [clojure.test :refer :all]
            [com.staralliance.flights :refer :all]))


(testing "Cypher"
  (deftest test-cypher-create-trip-one-way
    (let [trip [666
                "8X45OI"
                {:source "LHR",
                 :leave "07:30",
                 :destination "CPH",
                 :arrive "11:10",
                 :carrier "BA",
                 :flight "1638"}
                97]

          expected (clojure.string/join " \n"
                     ["MATCH (da:Airport {iata: \"LHR\"})"
                      "MATCH (aa:Airport {iata: \"CPH\"})"
                      "MERGE (p:Person {pid: 666})"
                      "CREATE (da)<-[:DEPARTS]-(f:Flight {no: \"BA1638\", departs_at: \"2016-04-12T07:30:00Z\", arrives_at: \"2016-04-12T11:10:00Z\"})-[:ARRIVES]->(aa)"
                      "CREATE (f)<-[:SEGMENT]-(t:Trip {pnr: \"8X45OI\", name: \"8X45OI\"})"
                      "CREATE (p)-[:TRAVELS]->(t)"
                      "CREATE (p)-[:EXPECTED_AT {at: 1460459400000}]->(aa)"
                      "CREATE (p)-[:TRANSMITS   {at: 1460446200000}]->(:Point:Location {latitude: da.latitude, longitude: da.longitude})"])]
      (is (= expected (cypher-create-trip trip (clj-time.core/date-time 2016 07 18)))))))

(testing "Datetimes"
  (deftest test-add-timestamps-to-flight
    (let [input     {:source "AAL", :leave "07:15",             :destination "CPH", :arrive "08:05",             :carrier "SK", :flight "1202"}
          expected  {:source "AAL", :leave "2015-12-21T07:15Z", :destination "CPH", :arrive "2015-12-21T08:05Z", :carrier "SK", :flight "1202"}]
      (is
        (= expected
           (timestamp-trip input 11 (clj-time.core/date-time 2016)))))))
