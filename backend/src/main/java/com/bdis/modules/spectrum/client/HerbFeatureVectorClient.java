package com.bdis.modules.spectrum.client;

import com.bdis.modules.herb.entity.HerbImageEntity;
import java.util.List;

public interface HerbFeatureVectorClient {

    List<Double> extract(HerbImageEntity image);
}
