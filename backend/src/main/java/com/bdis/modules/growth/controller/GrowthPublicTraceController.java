package com.bdis.modules.growth.controller;

import com.bdis.common.core.Result;
import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.vo.GrowthPublicTraceArchiveVO;
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
@RequestMapping("/trace/growth")
public class GrowthPublicTraceController {

    private final GrowthRecordService growthRecordService;

    public GrowthPublicTraceController(GrowthRecordService growthRecordService) {
        this.growthRecordService = growthRecordService;
    }

    @GetMapping("/{traceCode}")
    public Result<GrowthPublicTraceArchiveVO> publicTrace(@PathVariable String traceCode) {
        return Result.success(growthRecordService.publicTrace(traceCode));
    }

    @GetMapping("/{traceCode}/qrcode")
    public ResponseEntity<Resource> publicQrCode(@PathVariable String traceCode) {
        return fileResponse(growthRecordService.publicTraceQrCode(traceCode));
    }

    @GetMapping("/{traceCode}/images/{imageId}")
    public ResponseEntity<Resource> publicImage(
            @PathVariable String traceCode, @PathVariable Long imageId) {
        return fileResponse(growthRecordService.publicTraceImage(traceCode, imageId));
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
