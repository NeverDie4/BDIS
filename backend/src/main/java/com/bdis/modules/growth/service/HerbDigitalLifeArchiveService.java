package com.bdis.modules.growth.service;

import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifePublicArchiveVO;

public interface HerbDigitalLifeArchiveService {

    HerbDigitalLifeArchiveVO getByTaskId(Long taskId);

    HerbDigitalLifePublicArchiveVO publicArchive(String traceCode);

    FileContentVO publicQrCode(String traceCode);

    FileContentVO publicImage(String traceCode, Long imageId);
}
