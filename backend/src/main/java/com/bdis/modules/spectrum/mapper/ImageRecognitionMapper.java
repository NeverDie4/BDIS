package com.bdis.modules.spectrum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.spectrum.dto.HerbRecognitionQueryRequest;
import com.bdis.modules.spectrum.entity.ImageRecognitionEntity;
import com.bdis.modules.spectrum.vo.HerbRecognitionVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ImageRecognitionMapper extends BaseMapper<ImageRecognitionEntity> {

    int insertRecognition(ImageRecognitionEntity recognition);

    HerbRecognitionVO selectLatestByImageId(@Param("imageId") Long imageId);

    HerbRecognitionVO selectVoById(@Param("id") Long id);

    List<HerbRecognitionVO> selectByImageId(@Param("imageId") Long imageId);

    Long countPage(@Param("query") HerbRecognitionQueryRequest query);

    List<HerbRecognitionVO> selectPage(
            @Param("query") HerbRecognitionQueryRequest query,
            @Param("offset") Long offset,
            @Param("pageSize") Integer pageSize);
}
