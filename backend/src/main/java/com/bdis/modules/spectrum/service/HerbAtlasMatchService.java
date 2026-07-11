package com.bdis.modules.spectrum.service;

import com.bdis.modules.herb.entity.HerbImageEntity;
import java.util.List;

public interface HerbAtlasMatchService {

    List<HerbAtlasMatchResult> matchTopN(HerbImageEntity image, int topN);
}
