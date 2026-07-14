package com.bdis.modules.growth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.dictionary.support.DictionaryReferenceValidator;
import com.bdis.modules.growth.dto.GrowthRecordCreateRequest;
import com.bdis.modules.growth.mapper.GrowthAuditRecordMapper;
import com.bdis.modules.growth.mapper.GrowthRecordMapper;
import com.bdis.modules.growth.mapper.GrowthTraceEventMapper;
import com.bdis.modules.growth.service.impl.GrowthRecordServiceImpl;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.mapper.HerbMapper;
import com.bdis.modules.map.entity.MapPointEntity;
import com.bdis.modules.map.mapper.MapPointMapper;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GrowthRecordPointStatusTest {

    @Mock private GrowthRecordMapper growthRecordMapper;
    @Mock private GrowthAuditRecordMapper growthAuditRecordMapper;
    @Mock private GrowthTraceEventMapper growthTraceEventMapper;
    @Mock private HerbBatchMapper herbBatchMapper;
    @Mock private HerbCollectionTaskMapper herbCollectionTaskMapper;
    @Mock private MapPointMapper mapPointMapper;
    @Mock private HerbMapper herbMapper;
    @Mock private HerbImageMapper herbImageMapper;
    @Mock private UserMapper userMapper;
    @Mock private DataScopeService dataScopeService;
    @Mock private DictionaryReferenceValidator dictionaryReferenceValidator;
    @Mock private CollectionAccessService collectionAccessService;
    @Mock private FileResourceService fileResourceService;

    private GrowthRecordService service;

    @BeforeEach
    void setUp() {
        service =
                new GrowthRecordServiceImpl(
                        growthRecordMapper,
                        growthAuditRecordMapper,
                        growthTraceEventMapper,
                        herbBatchMapper,
                        herbCollectionTaskMapper,
                        mapPointMapper,
                        herbMapper,
                        herbImageMapper,
                        userMapper,
                        dataScopeService,
                        dictionaryReferenceValidator,
                        collectionAccessService,
                        fileResourceService);
    }

    @Test
    void disabledPointRejectsNewCollectionRecord() {
        MapPointEntity point = new MapPointEntity();
        point.setId(7L);
        point.setStatus(0);
        when(mapPointMapper.selectById(7L)).thenReturn(point);

        assertThatThrownBy(() -> service.createForPoint(7L, new GrowthRecordCreateRequest()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                org.assertj.core.api.Assertions.assertThat(exception.getMessage())
                                        .contains("停用"));
    }
}
