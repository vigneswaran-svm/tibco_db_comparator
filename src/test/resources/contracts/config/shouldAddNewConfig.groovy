import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should add new configuration"
    description "POST /api/compare/configs creates a new comparator config record"

    request {
        method POST()
        url "/api/compare/configs"
        headers {
            contentType applicationJson()
            accept applicationJson()
        }
        body([
            serviceId                : "TESTSERVICE",
            tableName                : "TEST_TABLE",
            tableFields              : "FIELD1,FIELD2,FIELD3",
            primaryFields            : "FIELD1",
            comparatorExecutionStatus: "N",
            startDate                : "2026-02-01T00:00:00",
            endDate                  : "2026-02-10T23:59:59"
        ])
    }

    response {
        status CREATED()
        headers {
            contentType applicationJson()
        }
        body([
            id                       : $(anyNumber()),
            serviceId                : "TESTSERVICE",
            tableName                : "TEST_TABLE",
            tableFields              : "FIELD1,FIELD2,FIELD3",
            primaryFields            : "FIELD1",
            comparatorExecutionStatus: "N"
        ])
    }
}