package com.bdis.soap.component;

import com.bdis.common.exception.SoapExchangeException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class SoapXmlParser {

    public Map<String, Object> parseGrowthRecord(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document =
                    factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("externalNo", text(document, "externalNo"));
            result.put("herbName", text(document, "herbName"));
            result.put("baseName", text(document, "baseName"));
            result.put("collectorName", text(document, "collectorName"));
            result.put("collectedAt", text(document, "collectedAt"));
            result.put("sourceType", text(document, "sourceType"));
            return result;
        } catch (Exception exception) {
            throw new SoapExchangeException("SOAP XML 解析失败", exception);
        }
    }

    private String text(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }
}
