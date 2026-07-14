package com.bdis.modules.training.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class TrainingParticipantBatchResultVO {
    private int requestedCount;
    private int uniqueUserCount;
    private int successCount;
    private int duplicateCount;
    private int failureCount;
    private List<Long> successUserIds = new ArrayList<>();
    private List<Long> duplicateUserIds = new ArrayList<>();
    private List<TrainingParticipantFailureVO> failures = new ArrayList<>();
}
