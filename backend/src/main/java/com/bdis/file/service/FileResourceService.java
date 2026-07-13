package com.bdis.file.service;

import com.bdis.common.core.PageResult;
import com.bdis.file.dto.FileUploadDTO;
import com.bdis.file.query.FileResourceQuery;
import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.file.vo.FileResourceVO;
import java.nio.file.Path;

public interface FileResourceService {

    FileResourceVO upload(FileUploadDTO dto);

    FileResourceVO importPublic(Path sourceFile, String originalFilename, String remark);

    FileResourceVO registerPublic(String existingFileUrl, String originalFilename, String remark);

    PageResult<FileResourceVO> page(FileResourceQuery query);

    FileResourceVO detail(Long fileId);

    FileContentVO content(Long fileId, String disposition);

    FileContentVO publicContent(Long fileId);

    Path resolveLocalPath(String fileUrl);

    Long resolveFileId(String fileUrl);

    void delete(Long fileId);

    void deleteOwnUnboundUpload(Long fileId);

    void deleteSystem(Long fileId);
}
