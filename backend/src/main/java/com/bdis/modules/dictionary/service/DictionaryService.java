package com.bdis.modules.dictionary.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.dictionary.dto.DictItemRequest;
import com.bdis.modules.dictionary.dto.DictTypeRequest;
import com.bdis.modules.dictionary.dto.RegionRequest;
import com.bdis.modules.dictionary.vo.DictItemVO;
import com.bdis.modules.dictionary.vo.DictTypeVO;
import com.bdis.modules.dictionary.vo.RegionVO;
import java.util.List;

public interface DictionaryService {
    PageResult<DictTypeVO> pageTypes(long page, long size, String keyword, Integer status);

    Long createType(DictTypeRequest request);

    void updateType(Long typeId, DictTypeRequest request);

    void deleteType(Long typeId);

    List<DictItemVO> items(String typeCode, Integer status);

    Long createItem(String typeCode, DictItemRequest request);

    void updateItem(String typeCode, Long itemId, DictItemRequest request);

    void deleteItem(String typeCode, Long itemId);

    List<RegionVO> regionTree(Integer status);

    Long createRegion(RegionRequest request);

    void updateRegion(Long regionId, RegionRequest request);

    void deleteRegion(Long regionId);
}
