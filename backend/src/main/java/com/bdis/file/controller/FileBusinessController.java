package com.bdis.file.controller;

import com.bdis.common.response.ApiResponse;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.vo.FileBusinessVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file-relations")
public class FileBusinessController {

    private final FileBusinessService fileBusinessService;

    public FileBusinessController(FileBusinessService fileBusinessService) {
        this.fileBusinessService = fileBusinessService;
    }

    @PostMapping
    public ApiResponse<FileBusinessVO> bind(@Valid @RequestBody FileBusinessBindDTO dto) {
        return ApiResponse.success(fileBusinessService.bind(dto));
    }

    @DeleteMapping("/{relationId}")
    public ApiResponse<Void> unbind(@PathVariable Long relationId) {
        fileBusinessService.unbind(relationId);
        return ApiResponse.success();
    }
}
