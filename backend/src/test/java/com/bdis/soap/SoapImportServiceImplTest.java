package com.bdis.soap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.SoapExchangeException;
import com.bdis.soap.component.SoapXmlParser;
import com.bdis.soap.integration.SoapBusinessImportHandler;
import com.bdis.soap.service.impl.SoapImportServiceImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SoapImportServiceImplTest {

    @Mock private SoapXmlParser soapXmlParser;
    @Mock private SoapBusinessImportHandler importHandler;

    @Test
    void rejectsCampusBusinessFailureBeforeImportingData() {
        when(soapXmlParser.parseGrowthRecord("<response/>"))
                .thenReturn(Map.of("code", "FAILED", "message", "校内系统拒绝请求"));
        SoapImportServiceImpl service =
                new SoapImportServiceImpl(soapXmlParser, List.of(importHandler));

        assertThatThrownBy(() -> service.parseAndPrepareImport("GROWTH_RECORD", "<response/>"))
                .isInstanceOf(SoapExchangeException.class)
                .hasMessageContaining("校内系统拒绝请求");
        verifyNoInteractions(importHandler);
    }
}
