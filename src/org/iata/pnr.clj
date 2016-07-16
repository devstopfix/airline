(ns org.iata.pnr
  (:require [clojure.test.check.generators :as gen]))
;
; PNR - Passenger Name Record
;
; -- https://en.wikipedia.org/wiki/Passenger_name_record
;

(def pnr-re #"[0-9A-Z]{6}")

(def pnr-length 6)

(defn valid-pnr? [^String s]
  (some? (re-matches pnr-re s)))


;
; test.check generator
;

(def char-alphanumeric-uppercase
  "Generate uppercase alphanumeric characters."
  (gen/fmap char
        (gen/one-of [(gen/choose 48 57) (gen/choose 65 90)])))

(defn- vector-of-chars-to-string [vc]
  (apply str vc))

(def pnr-generator
  (gen/fmap vector-of-chars-to-string
            (gen/vector char-alphanumeric-uppercase pnr-length)))