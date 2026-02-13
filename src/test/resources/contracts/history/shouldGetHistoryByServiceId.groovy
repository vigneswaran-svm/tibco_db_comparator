import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should get history filtered by serviceId"
    description "GET /api/compare/history?serviceId=X returns filtered history"

    request {
        method GET()
        urlPath("/api/compare/history") {
            queryParameters {
                parameter "serviceId": "CORRESPONDENCEREQUEST"
            }
        }
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