package com.bdis.soap.campus;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "queryGrowthRecordsRequest", namespace = CampusGrowthDataPort.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class CampusGrowthQueryRequest {

    @XmlElement(namespace = CampusGrowthDataPort.NAMESPACE, required = true)
    private String requestId;

    @XmlElement(namespace = CampusGrowthDataPort.NAMESPACE)
    private String since;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }
}
