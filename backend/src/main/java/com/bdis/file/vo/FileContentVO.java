package com.bdis.file.vo;

import lombok.Data;
import org.springframework.core.io.Resource;

@Data
public class FileContentVO {

    private Resource resource;
    private String fileName;
    private String contentType;
    private Long fileSize;
}
