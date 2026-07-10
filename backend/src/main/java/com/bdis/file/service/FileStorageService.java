package com.bdis.file.service;

import com.bdis.file.vo.FileContentVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFile save(MultipartFile file);

    FileContentVO load(String storagePath, String fileName, String contentType, Long fileSize);

    void delete(String storagePath);

    record StoredFile(String storedName, String storagePath, String fileUrl) {}
}
