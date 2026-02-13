import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should execute comparison with service filter"
    description "POST /api/compare/execute with serviceName filter returns comparison result"

    request {
        method POST()
        url "/api/compare/execute"
        headers {
            contentType applicationJson()
            accept applicationJson()
        }
        body([
            serviceName: "CORRESPONDENCEREQUEST",
            tableName  : ""
        ])
    }

    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body([
            status                   : $(anyNonBlankString()),
            message                  : $(anyNonBlankString()),
            totalRecordsMsDb         : $(anyNumber()),
            totalRecordsSoiDb        : $(anyNumber()),
            totalComparedSuccessCount: $(anyNumber()),
            totalComparedFailureCount: $(anyNumber()),
            configsProcessed         : $(anyNumber()),
            serviceResults           : $(anyNonEmptyString()),
            error                    : null
        ])
    }
}