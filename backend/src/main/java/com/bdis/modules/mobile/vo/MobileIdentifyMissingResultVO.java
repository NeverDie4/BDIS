package com.bdis.modules.mobile.vo;

import java.util.List;
import lombok.Data;

@Data
public class MobileIdentifyMissingResultVO {

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private List<MobileIdentifyMissingItemVO> items;
}
