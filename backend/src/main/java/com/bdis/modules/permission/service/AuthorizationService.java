package com.bdis.modules.permission.service;

import com.bdis.modules.permission.dto.AuthorizationDecisionDTO;
import com.bdis.modules.permission.vo.AuthorizationDecisionVO;
import com.bdis.modules.permission.vo.MenuVO;
import com.bdis.modules.permission.vo.PermissionVO;
import java.util.List;

public interface AuthorizationService {

    boolean hasPermission(String permissionCode);

    void requirePermission(String permissionCode);

    AuthorizationDecisionVO decide(AuthorizationDecisionDTO dto);

    List<MenuVO> currentMenus();

    List<PermissionVO> currentButtons(String menuCode);
}
