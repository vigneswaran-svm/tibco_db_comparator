import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should clear comparison history"
    description "DELETE /api/compare/history clears all history records"

    request {
        method DELETE()
        url "/api/compare/history"
    }

    response {
        status OK()
        body("Comparison history cleared successfully")
    }
}