package com.bdis.file.service;

import com.bdis.common.response.PageResult;
import com.bdis.file.dto.FileUploadDTO;
import com.bdis.file.query.FileResourceQuery;
import com.bdis.file.vo.FileContentVO;
import com.bdis.file.vo.FileResourceVO;

public interface FileResourceService {

    FileResourceVO upload(FileUploadDTO dto);

    PageResult<FileResourceVO> page(FileResourceQuery query);

    FileResourceVO detail(Long fileId);

    FileContentVO content(Long fileId, String disposition);

    void delete(Long fileId);
}
