package com.bdis.file.service.impl;

import com.bdis.common.exception.FileStorageException;
import com.bdis.file.service.FileStorageService;
import com.bdis.file.vo.FileContentVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final Path storageRoot;

    public LocalFileStorageServiceImpl(
            @Value("${bdis.file.storage-path:./storage}") String storagePath) {
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("文件不能为空");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + (extension == null ? "" : "." + extension);
        Path relativeDir = Path.of("uploads", LocalDate.now().toString());
        Path targetDir = storageRoot.resolve(relativeDir).normalize();
        Path targetPath = targetDir.resolve(storedName).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            throw new FileStorageException("文件存储路径不合法");
        }
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new FileStorageException("文件保存失败", exception);
        }
        String storagePath = storageRoot.relativize(targetPath).toString().replace('\\', '/');
        return new StoredFile(storedName, storagePath, "/api/files/" + storagePath);
    }

    @Override
    public StoredFile save(Path sourceFile) {
        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new FileStorageException("文件不存在或不可读取");
        }
        String extension = StringUtils.getFilenameExtension(sourceFile.getFileName().toString());
        String storedName = UUID.randomUUID() + (extension == null ? "" : "." + extension);
        Path relativeDir = Path.of("uploads", LocalDate.now().toString());
        Path targetDir = storageRoot.resolve(relativeDir).normalize();
        Path targetPath = targetDir.resolve(storedName).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            throw new FileStorageException("文件存储路径不合法");
        }
        try {
            Files.createDirectories(targetDir);
            Files.copy(sourceFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new FileStorageException("文件保存失败", exception);
        }
        String storagePath = storageRoot.relativize(targetPath).toString().replace('\\', '/');
        return new StoredFile(storedName, storagePath, "/api/files/" + storagePath);
    }

    @Override
    public FileContentVO load(
            String storagePath, String fileName, String contentType, Long fileSize) {
        Path targetPath = resolve(storagePath);
        Resource resource = new FileSystemResource(targetPath);
        FileContentVO vo = new FileContentVO();
        vo.setResource(resource);
        vo.setFileName(fileName);
        vo.setContentType(contentType);
        vo.setFileSize(fileSize);
        return vo;
    }

    @Override
    public Path resolve(String storagePathOrUrl) {
        Path targetPath = storageRoot.resolve(normalizeStoragePath(storagePathOrUrl)).normalize();
        if (!targetPath.startsWith(storageRoot) || !Files.isRegularFile(targetPath)) {
            throw new FileStorageException("文件不存在或已不可访问");
        }
        return targetPath;
    }

    @Override
    public void delete(String storagePath) {
        Path targetPath = storageRoot.resolve(normalizeStoragePath(storagePath)).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException exception) {
            throw new FileStorageException("文件删除失败", exception);
        }
    }

    private String normalizeStoragePath(String storagePathOrUrl) {
        if (storagePathOrUrl == null || storagePathOrUrl.isBlank()) {
            throw new FileStorageException("文件存储路径不能为空");
        }
        String normalized = storagePathOrUrl.replace('\\', '/');
        if (normalized.startsWith("/api/files/")) {
            return normalized.substring("/api/files/".length());
        }
        if (normalized.startsWith("/files/")) {
            return normalized.substring("/files/".length());
        }
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }
}
