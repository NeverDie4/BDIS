package com.bdis.soap.campus;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "queryGrowthRecordsResponse", namespace = CampusGrowthDataPort.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class CampusGrowthQueryResponse {

    @XmlElement(namespace = CampusGrowthDataPort.NAMESPACE, required = true)
    private String code;

    @XmlElement(namespace = CampusGrowthDataPort.NAMESPACE, required = true)
    private String message;

    @XmlElementWrapper(name = "records", namespace = CampusGrowthDataPort.NAMESPACE)
    @XmlElement(name = "record", namespace = CampusGrowthDataPort.NAMESPACE)
    private List<CampusGrowthRecord> records = new ArrayList<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<CampusGrowthRecord> getRecords() {
        return records;
    }

    public void setRecords(List<CampusGrowthRecord> records) {
        this.records = records == null ? new ArrayList<>() : records;
    }
}
