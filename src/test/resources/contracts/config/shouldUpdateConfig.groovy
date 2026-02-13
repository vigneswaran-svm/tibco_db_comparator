import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should update existing configuration"
    description "PUT /api/compare/configs/update updates config with id in request body"

    request {
        method PUT()
        url "/api/compare/configs/update"
        headers {
            contentType applicationJson()
            accept applicationJson()
        }
        body([
            id                       : 1,
            serviceId                : "UPDATEDSERVICE",
            tableName                : "UPDATED_TABLE",
            tableFields              : "FIELD1,FIELD2",
            primaryFields            : "FIELD1",
            comparatorExecutionStatus: "N",
            startDate                : "2026-02-01T00:00:00",
            endDate                  : "2026-02-10T23:59:59"
        ])
    }

    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body([
            id       : 1,
            serviceId: "UPDATEDSERVICE",
            tableName: "UPDATED_TABLE"
        ])
    }
}