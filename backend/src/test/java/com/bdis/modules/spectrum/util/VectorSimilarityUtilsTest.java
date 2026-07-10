package com.bdis.modules.spectrum.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bdis.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class VectorSimilarityUtilsTest {

    @Test
    void cosineSimilarityReturnsOneForSameDirection() {
        double result = VectorSimilarityUtils.cosineSimilarity(List.of(1D, 0D), List.of(2D, 0D));

        assertThat(result).isEqualTo(1D);
    }

    @Test
    void cosineSimilarityRejectsDimensionMismatch() {
        assertThatThrownBy(
                        () -> VectorSimilarityUtils.cosineSimilarity(List.of(1D, 0D), List.of(1D)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dimension mismatch");
    }
}
