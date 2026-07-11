package com.bdis.audit.dto;

import lombok.Data;

@Data
public class FileAccessRecordDTO {

    private Long fileId;
    private String accessType;
    private String accessResult = "SUCCESS";
}
