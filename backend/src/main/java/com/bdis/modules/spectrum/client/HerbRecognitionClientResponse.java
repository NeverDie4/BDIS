package com.bdis.modules.spectrum.client;

import com.bdis.modules.spectrum.vo.HerbRecognitionCandidateVO;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class HerbRecognitionClientResponse {

    private Boolean success;

    private String predictedName;

    private BigDecimal confidence;

    private List<HerbRecognitionCandidateVO> candidates;

    private String rawResult;
}
