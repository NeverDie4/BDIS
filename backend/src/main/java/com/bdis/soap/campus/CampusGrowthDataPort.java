package com.bdis.soap.campus;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

@WebService(name = "CampusGrowthDataPort", targetNamespace = CampusGrowthDataPort.NAMESPACE)
@SOAPBinding(
        style = SOAPBinding.Style.DOCUMENT,
        use = SOAPBinding.Use.LITERAL,
        parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface CampusGrowthDataPort {

    String NAMESPACE = "https://bdis.local/ws/campus-growth/v1";

    @WebMethod(operationName = "queryGrowthRecords", action = "queryGrowthRecords")
    @WebResult(
            name = "queryGrowthRecordsResponse",
            targetNamespace = NAMESPACE,
            partName = "parameters")
    CampusGrowthQueryResponse queryGrowthRecords(
            @WebParam(
                            name = "queryGrowthRecordsRequest",
                            targetNamespace = NAMESPACE,
                            partName = "parameters")
                    CampusGrowthQueryRequest request);
}
