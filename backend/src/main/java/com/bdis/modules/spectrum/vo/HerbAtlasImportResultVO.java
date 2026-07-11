package com.bdis.modules.spectrum.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class HerbAtlasImportResultVO {

    private Integer totalCount = 0;

    private Integer successCount = 0;

    private Integer skipCount = 0;

    private Integer failCount = 0;

    private List<HerbAtlasImportItemVO> items = new ArrayList<>();

    public void addSuccess(String fileName, String speciesCode, String message) {
        successCount++;
        items.add(HerbAtlasImportItemVO.of(fileName, speciesCode, "success", message));
    }

    public void addSkip(String fileName, String speciesCode, String message) {
        skipCount++;
        items.add(HerbAtlasImportItemVO.of(fileName, speciesCode, "skip", message));
    }

    public void addFail(String fileName, String speciesCode, String message) {
        failCount++;
        items.add(HerbAtlasImportItemVO.of(fileName, speciesCode, "fail", message));
    }

    public void increaseTotal() {
        totalCount++;
    }
}
