package com.bdis.modules.permission.service;

import com.bdis.modules.permission.dto.MenuDTO;
import com.bdis.modules.permission.query.MenuQuery;
import com.bdis.modules.permission.vo.MenuVO;
import java.util.List;

public interface MenuService {

    List<MenuVO> tree(MenuQuery query);

    MenuVO detail(Long id);

    Long create(MenuDTO dto);

    void update(Long id, MenuDTO dto);

    void delete(Long id);
}
