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
                 :flight "1638"} 97]

          expected "MATCH (da:Airport {iata: \"LHR\"}) MATCH (aa:Airport {iata: \"CPH\"}) MERGE (p:Person {pid: 666}) CREATE (da)<-[:DEPART]-(f:Flight {no: \"BA1638\"})-[:ARRIVE]->(aa) CREATE (f)<-[:SEGMENT]-(t:Trip {pnr: \"8X45OI\"})"]

      (is (= expected (cypher-create-trip trip))))))
