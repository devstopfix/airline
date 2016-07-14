(ns airline.dow
  (:use [clojure.set :only [difference]]))

(defn- sanitize [days]
  (if (nil? days)
    ""
    (-> days
      (.trim)
      (.toUpperCase))))

(defn- valid-str? [dow]
  (when dow
    (re-matches #"daily|Daily|DAILY|[1-7]{1,7}|X[1-7]{1,6}" dow)))

(defn digits [^String s]
  "Return a list of integers, one for
   each digit in the String. Non-digits
   are ignored."
  (->> s
    (seq)
    (filter #(Character/isDigit %))
    (map #(Character/digit % 10))))

(def daily
  (into (sorted-set) (range 1 (inc 7))))

(defn parse [^String s]
  "Convert a days-of-week String into a list
   of day indexes where Monday=1 and Sunday=7.
   Prefix of 'X' indicates exclusion of given days.
   e.g. '135' -> (1 3 5)"
  (when (valid-str? s)
    (let [days (sanitize s)]
      (cond
        (= days "DAILY")       (seq daily)
        (.startsWith days "X") (->>
                                 (digits days)
                                 (into #{})
                                 (difference daily)
                                 (seq))
        :else                  (digits days)))))
