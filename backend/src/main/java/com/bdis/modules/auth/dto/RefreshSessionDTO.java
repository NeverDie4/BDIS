package com.bdis.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshSessionDTO {

    @NotBlank private String refreshToken;
}
