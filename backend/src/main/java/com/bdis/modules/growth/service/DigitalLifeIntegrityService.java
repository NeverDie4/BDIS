package com.bdis.modules.growth.service;

import com.bdis.modules.growth.vo.DigitalLifeIntegrityVO;

public interface DigitalLifeIntegrityService {

    DigitalLifeIntegrityVO generate(Long taskId);

    DigitalLifeIntegrityVO verify(Long taskId);

    DigitalLifeIntegrityVO verifyPublic(String traceCode);
}
