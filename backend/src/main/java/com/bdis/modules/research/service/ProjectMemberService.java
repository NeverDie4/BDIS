package com.bdis.modules.research.service;

import com.bdis.modules.research.request.ProjectMemberAddRequest;
import com.bdis.modules.research.request.ProjectMemberUpdateRequest;
import com.bdis.modules.research.vo.ProjectMemberVO;
import java.util.List;

public interface ProjectMemberService {
    List<ProjectMemberVO> list(Long projectId, String memberStatus);

    ProjectMemberVO get(Long projectId, Long userId);

    Long add(Long projectId, ProjectMemberAddRequest request);

    void updateRole(Long projectId, Long userId, ProjectMemberUpdateRequest request);

    void remove(Long projectId, Long userId);
}
