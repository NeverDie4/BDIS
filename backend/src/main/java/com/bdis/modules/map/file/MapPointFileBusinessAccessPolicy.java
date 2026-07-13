package com.bdis.modules.map.file;

import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.modules.map.mapper.MapPointMapper;
import com.bdis.modules.permission.service.AuthorizationService;
import org.springframework.stereotype.Component;

@Component
public class MapPointFileBusinessAccessPolicy implements FileBusinessAccessPolicy {

    private final MapPointMapper mapPointMapper;
    private final AuthorizationService authorizationService;

    public MapPointFileBusinessAccessPolicy(
            MapPointMapper mapPointMapper, AuthorizationService authorizationService) {
        this.mapPointMapper = mapPointMapper;
        this.authorizationService = authorizationService;
    }

    @Override
    public String bizType() {
        return "map_point";
    }

    @Override
    public boolean exists(Long bizId) {
        return mapPointMapper.selectById(bizId) != null;
    }

    @Override
    public boolean canView(Long bizId) {
        return authorizationService.hasPermission("map:point:view");
    }

    @Override
    public boolean canAttach(Long bizId) {
        return authorizationService.hasPermission("map:point:create")
                || authorizationService.hasPermission("map:point:update");
    }

    @Override
    public boolean canDetach(Long bizId) {
        return authorizationService.hasPermission("map:point:update")
                || authorizationService.hasPermission("map:point:delete");
    }

    @Override
    public boolean canPublish(Long bizId) {
        return canAttach(bizId) || authorizationService.hasPermission("map:point:delete");
    }
}
