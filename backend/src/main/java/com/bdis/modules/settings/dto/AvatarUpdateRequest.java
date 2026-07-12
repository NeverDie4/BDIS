package com.bdis.modules.settings.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvatarUpdateRequest {

    @NotNull(message = "文件ID不能为空")
    private Long fileId;
}
