package com.bdis.file.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadDTO {

    @NotNull private MultipartFile file;

    private String fileType;
    private String bizType;
    private Long bizId;
    private String businessType;
    private Long businessId;
    private String fileUsage;
    private String remark;
}
