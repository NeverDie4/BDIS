package com.bdis.file.service;

import com.bdis.common.core.PageResult;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.modules.file.vo.FileResourceVO;

public interface FileBusinessService {

    FileBusinessVO bind(FileBusinessBindDTO dto);

    void unbind(Long relationId);

    void authorizeDeleteByFileId(Long fileId, boolean published);

    void deleteByFileId(Long fileId);

    void deleteByBusiness(String bizType, Long bizId);

    void deleteByBusinessAndFile(String bizType, Long bizId, Long fileId);

    boolean isBound(Long fileId, String bizType, Long bizId);

    PageResult<FileResourceVO> pageByBusiness(String bizType, Long bizId, long page, long size);
}
