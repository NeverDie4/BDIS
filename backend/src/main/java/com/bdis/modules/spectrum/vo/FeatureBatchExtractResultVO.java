package com.bdis.modules.spectrum.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FeatureBatchExtractResultVO {

    private Integer totalCount = 0;

    private Integer successCount = 0;

    private Integer skipCount = 0;

    private Integer failCount = 0;

    private List<FeatureBatchExtractItemVO> items = new ArrayList<>();

    public void addSuccess(FeatureBatchExtractItemVO item) {
        totalCount++;
        successCount++;
        item.setStatus("success");
        items.add(item);
    }

    public void addSkip(FeatureBatchExtractItemVO item) {
        totalCount++;
        skipCount++;
        item.setStatus("skip");
        items.add(item);
    }

    public void addFail(FeatureBatchExtractItemVO item) {
        totalCount++;
        failCount++;
        item.setStatus("failed");
        items.add(item);
    }
}
