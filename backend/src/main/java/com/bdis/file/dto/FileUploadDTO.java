package com.bdis.file.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "private", message = "上传文件只能先保存为 private")
    private String accessLevel = "private";

    private String remark;
}
