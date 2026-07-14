package com.bdis.modules.research.service;

import com.bdis.modules.research.request.ProjectMaterialBindRequest;
import com.bdis.modules.research.vo.ProjectMaterialVO;
import java.util.List;

public interface ProjectMaterialService {
    List<ProjectMaterialVO> list(Long projectId, String fileUsage);

    Long bind(Long projectId, ProjectMaterialBindRequest request);

    void unbind(Long projectId, Long fileId);
}
