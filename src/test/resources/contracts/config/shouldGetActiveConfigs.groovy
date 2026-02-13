import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should get active configurations"
    description "GET /api/compare/configs/active returns configs with status N"

    request {
        method GET()
        url "/api/compare/configs/active"
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