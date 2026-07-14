package com.bdis.modules.training.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingParticipantFailureVO {
    private Long userId;
    private String reason;
}
