package com.bdis.file.service;

import com.bdis.common.core.PageResult;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.modules.file.vo.FileResourceVO;
import java.util.List;

public interface FileBusinessService {

    FileBusinessVO bind(FileBusinessBindDTO dto);

    FileBusinessVO bindSystem(FileBusinessBindDTO dto);

    void unbind(Long relationId);

    void unbind(String bizType, Long bizId, Long fileId);

    void setPublicVisibility(Long fileId, String bizType, Long bizId, boolean publicVisible);

    void authorizeDeleteByFileId(Long fileId);

    void deleteByFileId(Long fileId);

    void deleteByBusiness(String bizType, Long bizId);

    void deleteByBusinessAndFile(String bizType, Long bizId, Long fileId);

    boolean isBound(Long fileId, String bizType, Long bizId);

    boolean existsByBusiness(String bizType, Long bizId);

    PageResult<FileResourceVO> pageByBusiness(String bizType, Long bizId, long page, long size);

    List<FileResourceVO> listByBusiness(String bizType, Long bizId);

    List<FileResourceVO> listByBusiness(String bizType, Long bizId, String fileUsage);

    List<FileBusinessVO> listBindingsByBusiness(String bizType, Long bizId);
}
