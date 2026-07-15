package com.bdis.modules.growth.controller;

import com.bdis.common.core.Result;
import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.growth.service.DigitalLifeIntegrityService;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.vo.DigitalLifeIntegrityVO;
import com.bdis.modules.growth.vo.HerbDigitalLifePublicArchiveVO;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/trace/digital-life")
public class HerbDigitalLifePublicArchiveController {

    private final HerbDigitalLifeArchiveService archiveService;
    private final DigitalLifeIntegrityService integrityService;

    public HerbDigitalLifePublicArchiveController(
            HerbDigitalLifeArchiveService archiveService,
            DigitalLifeIntegrityService integrityService) {
        this.archiveService = archiveService;
        this.integrityService = integrityService;
    }

    @GetMapping("/{traceCode}")
    public Result<HerbDigitalLifePublicArchiveVO> publicArchive(@PathVariable String traceCode) {
        return Result.success(archiveService.publicArchive(traceCode));
    }

    @GetMapping("/{traceCode}/integrity")
    public Result<DigitalLifeIntegrityVO> publicIntegrity(@PathVariable String traceCode) {
        return Result.success(integrityService.verifyPublic(traceCode));
    }

    @GetMapping("/{traceCode}/qrcode")
    public ResponseEntity<Resource> publicQrCode(@PathVariable String traceCode) {
        return fileResponse(archiveService.publicQrCode(traceCode));
    }

    @GetMapping("/{traceCode}/images/{imageId}")
    public ResponseEntity<Resource> publicImage(
            @PathVariable String traceCode, @PathVariable Long imageId) {
        return fileResponse(archiveService.publicImage(traceCode, imageId));
    }

    private ResponseEntity<Resource> fileResponse(FileContentVO content) {
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
