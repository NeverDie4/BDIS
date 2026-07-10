package com.bdis.soap.component;

import com.bdis.common.exception.SoapExchangeException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

@Component
public class SoapXmlParser {

    public Map<String, Object> parseGrowthRecord(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(throwingErrorHandler());
            Document document = builder.parse(new InputSource(new StringReader(xml)));
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

    private ErrorHandler throwingErrorHandler() {
        return new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXParseException {
                throw exception;
            }

            @Override
            public void error(SAXParseException exception) throws SAXParseException {
                throw exception;
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXParseException {
                throw exception;
            }
        };
    }
}
