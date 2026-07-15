package com.bdis.modules.performance.vo;

import com.bdis.modules.performance.entity.PerformanceParticipantEntity;
import com.bdis.modules.user.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceParticipantVO {

    private Long id;

    private Long performanceId;

    private Long userId;

    private String username;

    private String realName;

    private String participantRole;

    private Integer sortOrder;

    private Integer isPrimary;

    private String remark;

    public static PerformanceParticipantVO from(
            PerformanceParticipantEntity participant, UserEntity user) {
        PerformanceParticipantVO vo = new PerformanceParticipantVO();
        vo.setId(participant.getId());
        vo.setPerformanceId(participant.getPerformanceId());
        vo.setUserId(participant.getUserId());
        vo.setParticipantRole(participant.getParticipantRole());
        vo.setSortOrder(participant.getSortOrder());
        vo.setIsPrimary(participant.getIsPrimary());
        vo.setRemark(participant.getRemark());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
        }
        return vo;
    }
}
