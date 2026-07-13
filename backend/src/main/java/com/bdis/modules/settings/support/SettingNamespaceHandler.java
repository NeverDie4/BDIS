package com.bdis.modules.settings.support;

import com.bdis.common.security.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;

public interface SettingNamespaceHandler {

    String namespace();

    boolean supports(CurrentUser user);

    JsonNode defaultValues(CurrentUser user);

    JsonNode validateAndNormalize(JsonNode values, CurrentUser user);

    default int schemaVersion() {
        return 1;
    }
}
