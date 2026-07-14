package com.bdis.modules.soap.file;

import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.soap.mapper.SoapSyncTaskMapper;
import org.springframework.stereotype.Component;

@Component
public class SoapSyncTaskFileBusinessAccessPolicy implements FileBusinessAccessPolicy {

    private final SoapSyncTaskMapper soapSyncTaskMapper;
    private final AuthorizationService authorizationService;

    public SoapSyncTaskFileBusinessAccessPolicy(
            SoapSyncTaskMapper soapSyncTaskMapper, AuthorizationService authorizationService) {
        this.soapSyncTaskMapper = soapSyncTaskMapper;
        this.authorizationService = authorizationService;
    }

    @Override
    public String bizType() {
        return "soap_sync_task";
    }

    @Override
    public boolean exists(Long bizId) {
        return soapSyncTaskMapper.selectById(bizId) != null;
    }

    @Override
    public boolean canView(Long bizId) {
        return authorizationService.hasPermission("soap:exchange:view");
    }

    @Override
    public boolean canAttach(Long bizId) {
        return authorizationService.hasPermission("soap:exchange:execute");
    }

    @Override
    public boolean canDetach(Long bizId) {
        return canAttach(bizId);
    }

    @Override
    public boolean canPublish(Long bizId) {
        return false;
    }
}
