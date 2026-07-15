package com.bdis.soap.campus;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class CampusGrowthRecord {

    @XmlElement(namespace = CampusGrowthDataPort.NAMESPACE, required = true)
    private String externalNo;

    @XmlElement(namespace = CampusGrowthDataPort.NAMESPACE, required = true)
    private String herbName;

    @XmlElement(namespace = CampusGrowthDataPort.NAMESPACE, required = true)
    private String baseName;

    @XmlElement(namespace = CampusGrowthDataPort.NAMESPACE)
    private String collectorName;

    @XmlElement(namespace = CampusGrowthDataPort.NAMESPACE)
    private String collectedAt;

    @XmlElement(namespace = CampusGrowthDataPort.NAMESPACE)
    private String sourceType;

    public String getExternalNo() {
        return externalNo;
    }

    public void setExternalNo(String externalNo) {
        this.externalNo = externalNo;
    }

    public String getHerbName() {
        return herbName;
    }

    public void setHerbName(String herbName) {
        this.herbName = herbName;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getCollectorName() {
        return collectorName;
    }

    public void setCollectorName(String collectorName) {
        this.collectorName = collectorName;
    }

    public String getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(String collectedAt) {
        this.collectedAt = collectedAt;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
}
