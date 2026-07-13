package com.bdis.file.service;

import com.bdis.file.vo.FileContentVO;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFile save(MultipartFile file);

    StoredFile save(Path sourceFile);

    FileContentVO load(String storagePath, String fileName, String contentType, Long fileSize);

    Path resolve(String storagePathOrUrl);

    void delete(String storagePath);

    record StoredFile(String storedName, String storagePath) {}
}
