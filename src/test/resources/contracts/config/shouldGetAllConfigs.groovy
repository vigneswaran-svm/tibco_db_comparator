import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should get all configurations"
    description "GET /api/compare/configs returns list of all config records"

    request {
        method GET()
        url "/api/compare/configs"
        headers {
            accept applicationJson()
        }
    }

    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body($(anyNonEmptyString()))
    }
}