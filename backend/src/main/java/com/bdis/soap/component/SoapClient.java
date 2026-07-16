package com.bdis.soap.component;

import com.bdis.common.exception.SoapExchangeException;
import com.bdis.soap.campus.CampusGrowthDataPort;
import com.bdis.soap.config.SoapIntegrationProperties;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.soap.SOAPBinding;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.cxf.message.Message;
import org.springframework.stereotype.Component;

@Component
public class SoapClient {

    private static final QName SERVICE_NAME =
            new QName(CampusGrowthDataPort.NAMESPACE, "CampusGrowthDataService");
    private static final QName PORT_NAME =
            new QName(CampusGrowthDataPort.NAMESPACE, "CampusGrowthDataPort");

    private final SoapIntegrationProperties properties;

    public SoapClient(SoapIntegrationProperties properties) {
        this.properties = properties;
    }

    public String createGrowthQueryRequest(String requestId, LocalDateTime since) {
        try {
            SOAPMessage message =
                    MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createMessage();
            SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();
            SOAPBody body = envelope.getBody();
            SOAPElement request =
                    body.addChildElement(
                            new QName(
                                    CampusGrowthDataPort.NAMESPACE,
                                    "queryGrowthRecordsRequest",
                                    "camp"));
            addTextElement(request, "requestId", requestId);
            if (since != null) {
                addTextElement(request, "since", since.toString());
            }
            message.saveChanges();
            return toXml(message);
        } catch (SOAPException | IOException exception) {
            throw new SoapExchangeException("无法构造校内 SOAP 请求", exception);
        }
    }

    public String invoke(String requestXml) {
        verifyMockEndpoint();
        try {
            Service service = Service.create(SERVICE_NAME);
            service.addPort(
                    PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, properties.getCampusEndpoint());
            Dispatch<SOAPMessage> dispatch =
                    service.createDispatch(PORT_NAME, SOAPMessage.class, Service.Mode.MESSAGE);
            configureTimeout(dispatch.getRequestContext());
            SOAPMessage response = dispatch.invoke(toSoapMessage(requestXml));
            if (response.getSOAPBody().hasFault()) {
                throw new SoapExchangeException(
                        "本地模拟校内 SOAP 服务返回 Fault："
                                + response.getSOAPBody().getFault().getFaultString());
            }
            return toXml(response);
        } catch (SoapExchangeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SoapExchangeException("调用本地模拟校内 SOAP 服务失败", exception);
        }
    }

    private void addTextElement(SOAPElement parent, String name, String value)
            throws SOAPException {
        parent.addChildElement(new QName(CampusGrowthDataPort.NAMESPACE, name, "camp"))
                .addTextNode(value);
    }

    private SOAPMessage toSoapMessage(String xml) throws SOAPException, IOException {
        return MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL)
                .createMessage(
                        new MimeHeaders(),
                        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String toXml(SOAPMessage message) throws SOAPException, IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeTo(output);
        return output.toString(StandardCharsets.UTF_8);
    }

    private void configureTimeout(Map<String, Object> requestContext) {
        requestContext.put(Message.CONNECTION_TIMEOUT, properties.getConnectTimeoutMs());
        requestContext.put(Message.RECEIVE_TIMEOUT, properties.getReadTimeoutMs());
        requestContext.put(BindingProvider.SOAPACTION_USE_PROPERTY, true);
        requestContext.put(BindingProvider.SOAPACTION_URI_PROPERTY, "queryGrowthRecords");
        requestContext.put(MessageContext.HTTP_REQUEST_METHOD, "POST");
    }

    private void verifyMockEndpoint() {
        if (!properties.isMockMode()) {
            throw new SoapExchangeException("当前仅允许本地 mock SOAP 模式");
        }
        try {
            URI endpoint = new URI(properties.getCampusEndpoint());
            String host = endpoint.getHost();
            if (!"http".equalsIgnoreCase(endpoint.getScheme())
                    || !("localhost".equalsIgnoreCase(host)
                            || "127.0.0.1".equals(host)
                            || "::1".equals(host))) {
                throw new SoapExchangeException("mock SOAP 服务端点必须是本机 HTTP 地址");
            }
        } catch (URISyntaxException exception) {
            throw new SoapExchangeException("mock SOAP 服务端点格式不正确", exception);
        }
    }
}
