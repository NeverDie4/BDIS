package com.bdis.modules.settings.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingNamespaceVO {

    private Integer version;

    private Integer schemaVersion;

    private JsonNode values;
}
