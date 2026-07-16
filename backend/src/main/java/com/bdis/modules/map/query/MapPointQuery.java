package com.bdis.modules.map.query;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MapPointQuery {

    private String keyword;

    private String district;

    private Long speciesId;

    private Long baseId;

    private Boolean includeDisabled = false;
}
