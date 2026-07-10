package com.bdis.modules.spectrum.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbImageMatchPageVO {

    private Long id;

    private Long imageId;

    private String imageCode;

    private String imageUrl;

    private Long atlasId;

    private String atlasCode;

    private String atlasImageUrl;

    private Long speciesId;

    private String speciesName;

    private BigDecimal similarityScore;

    private Integer matchRank;

    private String matchResult;

    private String matchBatchNo;

    private LocalDateTime matchTime;

    private LocalDateTime createTime;
}
