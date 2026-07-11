package com.bdis.modules.spectrum.client;

import com.bdis.modules.herb.entity.HerbImageEntity;

public interface HerbRecognitionClient {

    HerbRecognitionClientResponse recognize(HerbImageEntity image);
}
