import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should reject config update without id"
    description "PUT /api/compare/configs/update returns 400 when id is missing"

    request {
        method PUT()
        url "/api/compare/configs/update"
        headers {
            contentType applicationJson()
            accept applicationJson()
        }
        body([
            serviceId                : "SOMESERVICE",
            tableName                : "SOME_TABLE",
            tableFields              : "FIELD1",
            primaryFields            : "FIELD1",
            comparatorExecutionStatus: "N",
            startDate                : "2026-02-01T00:00:00",
            endDate                  : "2026-02-10T23:59:59"
        ])
    }

    response {
        status BAD_REQUEST()
        body("Config id is required")
    }
}