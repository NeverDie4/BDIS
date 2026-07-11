package com.bdis.modules.spectrum.service;

import com.bdis.modules.spectrum.dto.HerbAtlasImportRequest;
import com.bdis.modules.spectrum.vo.HerbAtlasImportResultVO;

public interface HerbAtlasImportService {

    HerbAtlasImportResultVO importAtlas(HerbAtlasImportRequest request);

    int reconcileFileResources();
}
