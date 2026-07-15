package com.bdis.modules.performance.vo;

import com.bdis.modules.user.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceParticipantUserVO {

    private Long id;

    private String username;

    private String realName;

    public static PerformanceParticipantUserVO from(UserEntity user) {
        PerformanceParticipantUserVO vo = new PerformanceParticipantUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        return vo;
    }
}
