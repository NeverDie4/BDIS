package com.bdis.modules.map.service;

import com.bdis.modules.map.dto.MapPointUpsertRequest;
import com.bdis.modules.map.query.MapPointQuery;
import com.bdis.modules.map.vo.MapPointVO;
import java.util.List;

public interface MapPointService {

    List<MapPointVO> listMapPoints(MapPointQuery query);

    MapPointVO createMapPoint(MapPointUpsertRequest request);

    MapPointVO updateMapPoint(Long pointId, MapPointUpsertRequest request);

    void deleteMapPoint(Long pointId);
}
