package com.bdis.modules.herb.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.herb.dto.HerbImageQueryRequest;
import com.bdis.modules.herb.dto.HerbImageUpdateRequest;
import com.bdis.modules.herb.dto.HerbImageUploadRequest;
import com.bdis.modules.herb.vo.HerbImageVO;
import org.springframework.web.multipart.MultipartFile;

public interface HerbImageService {

    HerbImageVO upload(MultipartFile file, HerbImageUploadRequest request);

    HerbImageVO update(Long id, HerbImageUpdateRequest request);

    void delete(Long id);

    HerbImageVO getById(Long id);

    PageResult<HerbImageVO> page(HerbImageQueryRequest request);

    PageResult<HerbImageVO> my(Long collectorId, Integer pageNum, Integer pageSize);
}
