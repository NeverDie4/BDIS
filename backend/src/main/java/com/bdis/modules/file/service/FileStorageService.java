package com.bdis.modules.file.service;

import com.bdis.modules.file.vo.FileResourceVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileResourceVO upload(MultipartFile file, String bizType, String fileUsage);
}
