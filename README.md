
# stamp-duty-land-tax

This is the new stamp-duty-land-tax repository

## Running the service

Service Manager: `sm2 --start SDLT_ALL`

To run all tests and coverage: `sbt clean compile coverage test coverageOff coverageReport`

To start the server locally on `port 10913`: `sbt 'run 10913'`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").