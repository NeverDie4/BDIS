package com.bdis.modules.spectrum.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.herb.entity.HerbImageEntity;
import com.bdis.modules.spectrum.client.HerbFeatureVectorClient;
import com.bdis.modules.spectrum.entity.SpectrumEntity;
import com.bdis.modules.spectrum.mapper.HerbAtlasMapper;
import com.bdis.modules.spectrum.service.HerbAtlasMatchResult;
import com.bdis.modules.spectrum.service.HerbAtlasMatchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class HerbAtlasMatchServiceImpl implements HerbAtlasMatchService {

    private final HerbFeatureVectorClient featureVectorClient;
    private final HerbAtlasMapper herbAtlasMapper;
    private final ObjectMapper objectMapper;

    public HerbAtlasMatchServiceImpl(
            HerbFeatureVectorClient featureVectorClient,
            HerbAtlasMapper herbAtlasMapper,
            ObjectMapper objectMapper) {
        this.featureVectorClient = featureVectorClient;
        this.herbAtlasMapper = herbAtlasMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<HerbAtlasMatchResult> matchTopN(HerbImageEntity image, int topN) {
        List<Double> imageVector = featureVectorClient.extract(image);
        if (CollectionUtils.isEmpty(imageVector)) {
            return List.of();
        }
        List<SpectrumEntity> atlases = herbAtlasMapper.selectFeatureCandidates();
        if (CollectionUtils.isEmpty(atlases)) {
            return List.of();
        }
        List<HerbAtlasMatchResult> ranked =
                atlases.stream()
                        .map(atlas -> toMatchResult(atlas, imageVector))
                        .filter(match -> match.getSimilarity() != null)
                        .sorted(Comparator.comparing(HerbAtlasMatchResult::getSimilarity).reversed())
                        .limit(topN)
                        .toList();
        for (int index = 0; index < ranked.size(); index++) {
            ranked.get(index).setRank(index + 1);
        }
        return ranked;
    }

    private HerbAtlasMatchResult toMatchResult(SpectrumEntity atlas, List<Double> imageVector) {
        List<Double> atlasVector = parseVector(atlas.getFeatureVector());
        if (atlasVector.size() != imageVector.size()) {
            return new HerbAtlasMatchResult();
        }
        HerbAtlasMatchResult result = new HerbAtlasMatchResult();
        result.setAtlasId(atlas.getId());
        result.setSpeciesId(atlas.getSpeciesId());
        result.setSpeciesName(atlas.getHerbName());
        result.setSimilarity(toScore(cosineSimilarity(imageVector, atlasVector)));
        return result;
    }

    private List<Double> parseVector(String featureVector) {
        if (!StringUtils.hasText(featureVector)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(featureVector, new TypeReference<List<Double>>() {});
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Invalid atlas feature vector");
        }
    }

    private double cosineSimilarity(List<Double> left, List<Double> right) {
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int index = 0; index < left.size(); index++) {
            double leftValue = left.get(index);
            double rightValue = right.get(index);
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private BigDecimal toScore(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }
}
