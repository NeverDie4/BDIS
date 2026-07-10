package com.bdis.modules.spectrum.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.modules.spectrum.config.HerbRecognitionProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HerbRecognitionClientImplTest {

    @Test
    void toResponseParsesRecognizeServiceResultsArray() {
        HerbRecognitionClientImpl client =
                new HerbRecognitionClientImpl(
                        new HerbRecognitionProperties(), new ObjectMapper(), null);
        Map<String, Object> body =
                Map.of(
                        "results",
                        List.of(
                                Map.of(
                                        "rank",
                                        1,
                                        "speciesName",
                                        "五指毛桃（粗叶榕）",
                                        "confidence",
                                        0.7,
                                        "reason",
                                        "林下幼苗，叶片掌状深裂，新叶带绒毛")),
                        "needReview",
                        true,
                        "suggestion",
                        "建议结合根茎、植株被毛及花果特征进一步人工核验");

        HerbRecognitionClientResponse response =
                ReflectionTestUtils.invokeMethod(client, "toResponse", body);

        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getPredictedName()).isEqualTo("五指毛桃（粗叶榕）");
        assertThat(response.getConfidence()).isEqualByComparingTo(new BigDecimal("0.7"));
        assertThat(response.getCandidates()).hasSize(1);
        assertThat(response.getCandidates().get(0).getName()).isEqualTo("五指毛桃（粗叶榕）");
        assertThat(response.getCandidates().get(0).getRank()).isEqualTo(1);
        assertThat(response.getRawResult()).contains("五指毛桃");
    }

    @Test
    void toResponseParsesWrappedRecognizeServiceData() {
        HerbRecognitionClientImpl client =
                new HerbRecognitionClientImpl(
                        new HerbRecognitionProperties(), new ObjectMapper(), null);
        Map<String, Object> body =
                Map.of(
                        "code",
                        200,
                        "msg",
                        "识别成功",
                        "data",
                        Map.of(
                                "results",
                                List.of(
                                        Map.of(
                                                "rank",
                                                1,
                                                "speciesName",
                                                "五指毛桃（粗叶榕）",
                                                "confidence",
                                                0.7,
                                                "reason",
                                                "林下幼苗，叶片掌状深裂")),
                                "needReview",
                                true,
                                "suggestion",
                                "建议人工核验"));

        HerbRecognitionClientResponse response =
                ReflectionTestUtils.invokeMethod(client, "toResponse", body);

        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getPredictedName()).isEqualTo("五指毛桃（粗叶榕）");
        assertThat(response.getConfidence()).isEqualByComparingTo(new BigDecimal("0.7"));
        assertThat(response.getCandidates()).hasSize(1);
        assertThat(response.getRawResult()).contains("\"code\":200");
    }
}
