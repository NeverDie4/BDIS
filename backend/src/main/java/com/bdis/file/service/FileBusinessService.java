package com.bdis.file.service;

import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.modules.file.vo.FileResourceVO;
import java.util.List;

public interface FileBusinessService {

    FileBusinessVO bind(FileBusinessBindDTO dto);

    FileBusinessVO bindSystem(FileBusinessBindDTO dto);

    void unbind(Long relationId);

    void deleteByFileId(Long fileId);

    void deleteByBusiness(String bizType, Long bizId);

    void deleteByBusinessAndFile(String bizType, Long bizId, Long fileId);

    long countByFileId(Long fileId);

    List<FileResourceVO> listByBusiness(String bizType, Long bizId);

    List<FileBusinessVO> listBindingsByBusiness(String bizType, Long bizId);
}
