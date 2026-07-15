package com.bdis.modules.growth.service;

public interface DigitalLifeNarrationGenerator {

    GeneratedNarration generate(String stageInput, String templateNarration);

    record GeneratedNarration(String text, String source, String modelName) {}
}
