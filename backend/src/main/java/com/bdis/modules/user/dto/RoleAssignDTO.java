package com.bdis.modules.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleAssignDTO {

    @NotNull(message = "角色列表不能为空")
    private List<Long> roleIds;
}
