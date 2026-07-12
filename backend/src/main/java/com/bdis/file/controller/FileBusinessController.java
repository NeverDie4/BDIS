package com.bdis.file.controller;

import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
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
@RequirePermission("file:resource:update")
public class FileBusinessController {

    private final FileBusinessService fileBusinessService;

    public FileBusinessController(FileBusinessService fileBusinessService) {
        this.fileBusinessService = fileBusinessService;
    }

    @PostMapping
    public Result<FileBusinessVO> bind(@Valid @RequestBody FileBusinessBindDTO dto) {
        return Result.success(fileBusinessService.bind(dto));
    }

    @DeleteMapping("/{relationId}")
    public Result<Void> unbind(@PathVariable Long relationId) {
        fileBusinessService.unbind(relationId);
        return Result.success();
    }
}
