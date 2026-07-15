package com.bdis.modules.growth.service;

import com.bdis.modules.growth.vo.DigitalLifeNarrationGenerationVO;

public interface DigitalLifeNarrationService {

    DigitalLifeNarrationGenerationVO generateForTask(Long taskId);
}
