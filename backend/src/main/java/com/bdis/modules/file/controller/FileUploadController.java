package com.bdis.modules.file.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.file.service.FileStorageService;
import com.bdis.modules.file.vo.FileResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public Result<FileResourceVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String fileUsage) {
        return Result.success(fileStorageService.upload(file, bizType, fileUsage));
    }
}
