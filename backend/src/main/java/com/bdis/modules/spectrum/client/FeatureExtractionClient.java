package com.bdis.modules.spectrum.client;

import java.nio.file.Path;

public interface FeatureExtractionClient {

    FeatureExtractionClientResponse extract(Path imagePath);

    boolean isAvailable();
}
