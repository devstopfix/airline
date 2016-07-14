(ns airline.data.airports
  (:import  [java.io File])
  (:use     [clojure-csv.core :only [parse-csv]])
  (:require [clj-time.core :as t]))

(defn iata-to-tz [^File in]
  (with-open [rdr (clojure.java.io/reader in)]
    (reduce
      (fn [out row]
        (let [[iata tz] row]
          (assoc out iata (t/time-zone-for-id tz))))
      {}
      (doall (parse-csv rdr :delimiter \tab)))))
