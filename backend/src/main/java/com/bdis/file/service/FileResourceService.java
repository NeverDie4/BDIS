package com.bdis.file.service;

import com.bdis.common.core.PageResult;
import com.bdis.file.dto.FileUploadDTO;
import com.bdis.file.query.FileResourceQuery;
import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.file.vo.FileResourceVO;
import java.nio.file.Path;

public interface FileResourceService {

    FileResourceVO upload(FileUploadDTO dto);

    PageResult<FileResourceVO> page(FileResourceQuery query);

    FileResourceVO detail(Long fileId);

    FileContentVO content(Long fileId, String disposition);

    FileContentVO publicContent(Long fileId);

    Path resolveLocalPath(String fileUrl);

    Long resolveFileId(String fileUrl);

    void delete(Long fileId);
}
