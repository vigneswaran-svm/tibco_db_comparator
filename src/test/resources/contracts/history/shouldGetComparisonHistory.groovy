import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should get comparison history"
    description "GET /api/compare/history returns all history records"

    request {
        method GET()
        url "/api/compare/history"
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