# airline.data.flights

A Clojure library that generates a realistic schedule of flight data
from an airlines daily timetable.

## Input

The program requires an Airlines schedule in CSV format,
and the time-zones of the Airports serviced.

### Schedule Input File

A CSV file in this format:

```
"Source","Destination","Miles","Leave","Arrive","Carrier","Flight","Plane","Days","Stops","Meal","Notes"
AAL,CPH,148,06:25,07:10,SK,1228,M81,X67,0,N,""
```

Notes:

* the ```Arrive``` field may have a ```+1``` suffix to indicate next day
* a header row is assumed and skipped
* column names are not important
* the schedule may have duplicate rows with free-text descriptions used to 
differentiate them. These duplicates are excluded from the output based
on the key combination of [day source-airport flight]

### Airports Input File

There is a map of IATA codes to timezones at [Timezone map files](http://www.fresse.org/dateutils/tzmaps.html).

## Output

```
id,2014-10-06,AAL,SK1228,2014-10-06T06:25:00+01:00,CPH,2014-10-06T07:10:00+01:00
id,2014-10-07,AAL,SK1228,2014-10-07T06:25:00+01:00,CPH,2014-10-07T07:10:00+01:00
id,2014-10-08,AAL,SK1228,2014-10-08T06:25:00+01:00,CPH,2014-10-08T07:10:00+01:00
id,2014-10-09,AAL,SK1228,2014-10-09T06:25:00+01:00,CPH,2014-10-09T07:10:00+01:00
id,2014-10-10,AAL,SK1228,2014-10-10T06:25:00+01:00,CPH,2014-10-10T07:10:00+01:00
```

* A 63-bit identifier to be used as a surrogate key
* Day of flight
* Departure Airport or City IATA code
* Flight number
* Scheduled Departure date-time
* Destination IATA
* Scheduled Arrival date-time

## Build

```
git clone ...
lein uberjar
wget https://raw.github.com/hroptatyr/dateutils/tzmaps/iata.tzmap
```

## Usage

Generate 90 days of sample data, zipped:

```
java -jar target/airline.data.flights-0.4.2-standalone.jar \
    ~/etc/fresse.org/iata.tzmap \
    ~/etc/staralliance.com/StarAlliance200905.csv \
    2015-01-16 \
    90 | gzip -c > ~/etc/staralliance.com/flights-2015-01-16.csv.zip


java -jar target/airline.data.flights-0.4.2-standalone.jar \
    ~/etc/fresse.org/iata.tzmap \
    ~/etc/staralliance.com/StarAlliance200905.csv \
    2015-01-18 \
    90 > flights-2015-01-18.csv

```

Four parameters are required:

1. iata.tzmap file
2. schedule data csv file
3. date of first flight in format ````yyyy-mm-dd````
4. number of days of data to generate (e.g. 90)


# Libraries used

1. CSV reading by [clojure-csv](https://github.com/davidsantiago/clojure-csv)
2. [airline.dow](https://github.com/devstopfix/airline/tree/master/airline.dow)

# License

Copyright © 2014 Devstopfix

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
