package com.bdis.file.service;

import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.file.vo.FileResourceVO;
import java.util.List;

public interface FileBusinessService {

    FileBusinessVO bind(FileBusinessBindDTO dto);

    void unbind(Long relationId);

    List<FileResourceVO> listByBusiness(String bizType, Long bizId);
}
