package com.bdis.modules.mobile.vo;

import com.bdis.modules.spectrum.vo.HerbImageMatchCandidateVO;
import com.bdis.modules.spectrum.vo.HerbRecognitionVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class MobileImageIdentificationVO {

    private Long id;

    private Long imageId;

    private String imageCode;

    private String imageUrl;

    private String imageName;

    private String imageRole;

    private String collectPlace;

    private LocalDateTime collectTime;

    private Long finalSpeciesId;

    private String finalSpeciesName;

    private BigDecimal finalConfidence;

    private String resultSource;

    private String matchResult;

    private Boolean needReview;

    private String reviewStatus;

    private String suggestion;

    private List<HerbImageMatchCandidateVO> localCandidates;

    private HerbRecognitionVO doubaoRecognition;

    private LocalDateTime identifyTime;
}
