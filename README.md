
# stamp-duty-land-tax

This is the new stamp-duty-land-tax repository to coordinate business processing and submission.

For more information please refer to the [documentation](https://confluence.tools.tax.service.gov.uk/spaces/RBD/pages/1081606211/3.+Stamp+Duty+Land+Tax+-+SDLT).

## Running the service
Before starting, you will need to have  [service-manager](https://github.com/hmrc/service-manager) installed/configured

### Dependencies
All dependencies can be found in [AppDependencies.scala](https://github.com/hmrc/stamp-duty-land-tax/blob/main/project/AppDependencies.scala)

### Running locally:
Service Manager:
- Start dependent services `sm2 --start SDLT_ALL`
- Stop this service `sm2 --stop STAMP-DUTY-LAND-TAX`
- Start the server locally on `port 10913` with `sbt run`



How to switch service to use formP:

Execute next set of commands or run switchToFormP.sh

sm -stop STAMP-DUTY-LAND-TAX
sm --start STAMP-DUTY-LAND-TAX --appendArgs '{"STAMP-DUTY-LAND-TAX":["-Dfeatures.stub-formp-enabled=false"]}'

## Tool to generate random Returns data:

* Operation status:
  http://localhost:10914/stamp-duty-land-tax-stub/returns/getStatus

* Delete all data:
  http://localhost:10914/stamp-duty-land-tax-stub/returns/deleteAll

* Create a set of returns records:
  http://localhost:10914/stamp-duty-land-tax-stub/returns/createData?storn=STN001&returnType=inprogress&records=49

### Testing:
- Run unit tests: `sbt test`
- Run integration tests: `sbt it/test`
- To run all tests and coverage: `sbt clean compile coverage test it/test coverageOff coverageReport`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").