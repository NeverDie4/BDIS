package com.bdis.modules.settings.service;

import com.bdis.common.security.CurrentUser;
import com.bdis.modules.settings.dto.SettingUpdateRequest;
import com.bdis.modules.settings.vo.SettingNamespaceVO;
import java.util.Map;

public interface UserPreferenceService {

    Map<String, SettingNamespaceVO> getAll();

    SettingNamespaceVO get(String namespace);

    SettingNamespaceVO update(String namespace, SettingUpdateRequest request);

    SettingNamespaceVO reset(String namespace);

    String preferredLandingPath(CurrentUser user);
}
