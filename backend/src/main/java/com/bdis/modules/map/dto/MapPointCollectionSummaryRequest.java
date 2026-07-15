package com.bdis.modules.map.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MapPointCollectionSummaryRequest {

    @NotEmpty(message = "地图点位不能为空")
    @Size(max = 1000, message = "单次最多查询 1000 个地图点位")
    private List<@NotNull Long> pointIds;
}
