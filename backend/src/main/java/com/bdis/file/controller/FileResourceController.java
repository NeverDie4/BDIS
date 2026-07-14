package com.bdis.file.controller;

import com.bdis.audit.annotation.AuditLogAnnotation;
import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.file.dto.FileUploadDTO;
import com.bdis.file.query.FileResourceQuery;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.file.vo.FileResourceVO;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files")
@RequirePermission("file:resource:view")
public class FileResourceController {

    private final FileResourceService fileResourceService;

    public FileResourceController(FileResourceService fileResourceService) {
        this.fileResourceService = fileResourceService;
    }

    @PostMapping(
            path = {"", "/upload"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("file:resource:upload")
    @AuditLogAnnotation(
            module = "M05_FILE",
            operationType = "UPLOAD",
            description = "上传私有文件",
            bizType = "file_resource")
    public Result<FileResourceVO> upload(@Valid @ModelAttribute FileUploadDTO dto) {
        return Result.success(fileResourceService.upload(dto));
    }

    @GetMapping
    public Result<PageResult<FileResourceVO>> page(@Valid FileResourceQuery query) {
        return Result.success(fileResourceService.page(query));
    }

    @GetMapping("/{fileId}")
    public Result<FileResourceVO> detail(@PathVariable Long fileId) {
        return Result.success(fileResourceService.detail(fileId));
    }

    @GetMapping("/{fileId}/content")
    public ResponseEntity<Resource> content(
            @PathVariable Long fileId, @RequestParam(defaultValue = "inline") String disposition) {
        FileContentVO content = fileResourceService.content(fileId, disposition);
        ContentDisposition contentDisposition =
                ("attachment".equalsIgnoreCase(disposition)
                                ? ContentDisposition.attachment()
                                : ContentDisposition.inline())
                        .filename(content.getFileName(), StandardCharsets.UTF_8)
                        .build();
        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                content.getContentType() == null
                                        ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                                        : content.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(content.getResource());
    }

    @DeleteMapping("/{fileId}")
    @RequirePermission("file:resource:delete")
    @AuditLogAnnotation(
            module = "M05_FILE",
            operationType = "DELETE",
            description = "删除文件资源",
            bizType = "file_resource")
    public Result<Void> delete(@PathVariable Long fileId) {
        fileResourceService.delete(fileId);
        return Result.success();
    }

    @DeleteMapping("/{fileId}/unbound-upload")
    @RequirePermission("file:resource:upload")
    public Result<Void> deleteOwnUnboundUpload(@PathVariable Long fileId) {
        fileResourceService.deleteOwnUnboundUpload(fileId);
        return Result.success();
    }
}
