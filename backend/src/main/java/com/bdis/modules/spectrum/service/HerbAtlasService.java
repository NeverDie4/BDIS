package com.bdis.modules.spectrum.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.spectrum.dto.HerbAtlasQueryRequest;
import com.bdis.modules.spectrum.dto.HerbAtlasUpdateRequest;
import com.bdis.modules.spectrum.dto.HerbAtlasUploadRequest;
import com.bdis.modules.spectrum.vo.HerbAtlasTagVO;
import com.bdis.modules.spectrum.vo.HerbAtlasVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HerbAtlasService {

    HerbAtlasVO upload(MultipartFile file, HerbAtlasUploadRequest request);

    HerbAtlasVO update(Long id, HerbAtlasUpdateRequest request);

    void delete(Long id);

    HerbAtlasVO getById(Long id);

    PageResult<HerbAtlasVO> page(HerbAtlasQueryRequest request);

    List<HerbAtlasVO> listEnabled(Long speciesId);

    List<HerbAtlasTagVO> listTags(Long atlasId);
}
