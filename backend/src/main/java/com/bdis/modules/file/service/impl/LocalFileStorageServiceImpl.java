package com.bdis.modules.file.service.impl;

import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.file.service.FileStorageService;
import com.bdis.modules.file.vo.FileResourceVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final DateTimeFormatter DAY_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileResourceMapper fileResourceMapper;

    @Value("${bdis.file.storage-path:./storage}")
    private String storagePath;

    @Override
    public FileResourceVO upload(MultipartFile file, String bizType, String fileUsage) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        String fileNo = "FILE" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        String storedFilename = extension.isBlank() ? fileNo : fileNo + "." + extension;
        String relativeDir = "uploads/" + LocalDate.now().format(DAY_PATH_FORMATTER);
        Path root = Path.of(storagePath).toAbsolutePath().normalize();
        Path targetDir = root.resolve(relativeDir).normalize();
        Path target = targetDir.resolve(storedFilename).normalize();

        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }

        try {
            Files.createDirectories(targetDir);
            file.transferTo(target);
        } catch (IOException exception) {
            throw new IllegalStateException("文件保存失败", exception);
        }

        String fileUrl = "/api/files/" + relativeDir + "/" + storedFilename;
        FileResourceEntity entity = new FileResourceEntity();
        entity.setFileNo(fileNo);
        entity.setFileName(storedFilename);
        entity.setOriginalFilename(originalFilename);
        entity.setFileType(resolveFileType(file.getContentType()));
        entity.setFileFormat(extension);
        entity.setFileSize(file.getSize());
        entity.setFileUrl(fileUrl);
        entity.setThumbnailUrl(fileUrl);
        entity.setStorageType("local");
        entity.setUploadedAt(LocalDateTime.now());
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setRemark(buildRemark(bizType, fileUsage));
        fileResourceMapper.insert(entity);

        return toVO(entity);
    }

    private String buildRemark(String bizType, String fileUsage) {
        if (!StringUtils.hasText(bizType) && !StringUtils.hasText(fileUsage)) {
            return null;
        }
        return "bizType=" + nullToBlank(bizType) + ", fileUsage=" + nullToBlank(fileUsage);
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String resolveFileType(String contentType) {
        if (contentType == null) {
            return "file";
        }
        if (contentType.startsWith("image/")) {
            return "image";
        }
        if (contentType.startsWith("video/")) {
            return "video";
        }
        if (contentType.startsWith("audio/")) {
            return "audio";
        }
        if (contentType.contains("pdf") || contentType.contains("word") || contentType.contains("excel")) {
            return "document";
        }
        return "file";
    }

    private FileResourceVO toVO(FileResourceEntity entity) {
        FileResourceVO vo = new FileResourceVO();
        vo.setId(entity.getId());
        vo.setFileNo(entity.getFileNo());
        vo.setFileName(entity.getFileName());
        vo.setOriginalFilename(entity.getOriginalFilename());
        vo.setFileType(entity.getFileType());
        vo.setFileFormat(entity.getFileFormat());
        vo.setFileSize(entity.getFileSize());
        vo.setFileUrl(entity.getFileUrl());
        vo.setThumbnailUrl(entity.getThumbnailUrl());
        vo.setStorageType(entity.getStorageType());
        vo.setUploadedAt(entity.getUploadedAt());
        return vo;
    }
}
