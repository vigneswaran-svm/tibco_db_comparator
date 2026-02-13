import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should execute comparison with null request body"
    description "POST /api/compare/execute with empty body processes all active configs"

    request {
        method POST()
        url "/api/compare/execute"
        headers {
            contentType applicationJson()
            accept applicationJson()
        }
        body([
            serviceName: "",
            tableName  : ""
        ])
    }

    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body([
            status          : $(anyNonBlankString()),
            message         : $(anyNonBlankString()),
            configsProcessed: $(anyNumber())
        ])
    }
}