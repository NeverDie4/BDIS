package com.bdis.file.controller;

import com.bdis.file.service.FileResourceService;
import com.bdis.file.vo.FileContentVO;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public-files")
public class PublicFileController {

    private final FileResourceService fileResourceService;

    public PublicFileController(FileResourceService fileResourceService) {
        this.fileResourceService = fileResourceService;
    }

    @GetMapping("/{fileId}/content")
    public ResponseEntity<Resource> content(@PathVariable Long fileId) {
        FileContentVO content = fileResourceService.publicContent(fileId);
        ContentDisposition disposition =
                ContentDisposition.inline()
                        .filename(content.getFileName(), StandardCharsets.UTF_8)
                        .build();
        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                content.getContentType() == null
                                        ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                                        : content.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content.getResource());
    }
}
