package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BasicEntity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_image_match")
public class SpectrumComparisonEntity extends BasicEntity {

    private Long imageId;

    private Long recognitionId;

    private Long atlasId;

    private Long speciesId;

    private String herbName;

    private Integer matchRank;

    private BigDecimal imageSimilarity;

    private BigDecimal metadataScore;

    private BigDecimal finalScore;

    private String matchLevel;

    private Integer isImageTypeMatched;

    private Integer isGrowthStageMatched;

    private Integer isMedicinalPartMatched;

    private Integer isHealthStatusMatched;

    private Integer isFormTypeMatched;

    private String matchMethod;

    private String warningMsg;

    private String suggestion;
}
