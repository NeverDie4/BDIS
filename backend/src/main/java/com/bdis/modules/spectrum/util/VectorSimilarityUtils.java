package com.bdis.modules.spectrum.util;

import com.bdis.common.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.util.CollectionUtils;

public final class VectorSimilarityUtils {

    private VectorSimilarityUtils() {}

    public static double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (CollectionUtils.isEmpty(vectorA) || CollectionUtils.isEmpty(vectorB)) {
            throw new BusinessException("Feature vector is empty");
        }
        if (vectorA.size() != vectorB.size()) {
            throw new BusinessException("Feature vector dimension mismatch");
        }
        double dot = 0D;
        double normA = 0D;
        double normB = 0D;
        for (int index = 0; index < vectorA.size(); index++) {
            double left = vectorA.get(index);
            double right = vectorB.get(index);
            dot += left * right;
            normA += left * left;
            normB += right * right;
        }
        if (normA == 0D || normB == 0D) {
            throw new BusinessException("Feature vector norm is zero");
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static BigDecimal toScore(double similarity) {
        return BigDecimal.valueOf(similarity).setScale(4, RoundingMode.HALF_UP);
    }
}
