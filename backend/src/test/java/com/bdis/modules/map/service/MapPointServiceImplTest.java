package com.bdis.modules.map.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.security.CurrentUser;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbMapper;
import com.bdis.modules.map.dto.MapPointUpsertRequest;
import com.bdis.modules.map.entity.MapPointEntity;
import com.bdis.modules.map.mapper.MapPointMapper;
import com.bdis.modules.map.service.impl.MapPointServiceImpl;
import com.bdis.modules.map.vo.MapPointVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MapPointServiceImplTest {

    @Mock private MapPointMapper mapPointMapper;

    @Mock private HerbMapper herbMapper;

    @Mock private FileBusinessService fileBusinessService;

    @Mock private FileResourceService fileResourceService;

    private MapPointService mapPointService;

    @BeforeEach
    void setUp() {
        CurrentUser user =
                new CurrentUser(
                        8L,
                        "teacher01",
                        "教师一",
                        null,
                        null,
                        Set.of("TEACHER"),
                        Set.of(2L),
                        Set.of("map:point:create"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        mapPointService =
                new MapPointServiceImpl(
                        mapPointMapper, herbMapper, fileBusinessService, fileResourceService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBindsUploadedCoverAfterPointExistsAndPublishesThroughBusinessPolicy() {
        HerbEntity herb = new HerbEntity();
        herb.setId(3L);
        herb.setHerbName("黄连");
        when(herbMapper.selectById(3L)).thenReturn(herb);
        doAnswer(
                        invocation -> {
                            MapPointEntity entity = invocation.getArgument(0);
                            entity.setId(21L);
                            return 1;
                        })
                .when(mapPointMapper)
                .insert(any(MapPointEntity.class));
        when(fileResourceService.resolveFileId("/api/files/7/content")).thenReturn(7L);
        MapPointVO saved = new MapPointVO();
        saved.setId(21L);
        saved.setCoverImageUrl("/api/public-files/7/content");
        when(mapPointMapper.selectMapPoints(null, null, null, null)).thenReturn(List.of(saved));

        MapPointUpsertRequest request = new MapPointUpsertRequest();
        request.setSpeciesId(3L);
        request.setHerbName("黄连");
        request.setLongitude(BigDecimal.valueOf(106.5));
        request.setLatitude(BigDecimal.valueOf(29.5));
        request.setCoverImageUrl("/api/files/7/content");

        MapPointVO result = mapPointService.createMapPoint(request);

        assertThat(result.getCoverImageUrl()).isEqualTo("/api/public-files/7/content");
        ArgumentCaptor<FileBusinessBindDTO> bindCaptor =
                ArgumentCaptor.forClass(FileBusinessBindDTO.class);
        verify(fileBusinessService).bind(bindCaptor.capture());
        assertThat(bindCaptor.getValue().getFileId()).isEqualTo(7L);
        assertThat(bindCaptor.getValue().getBizType()).isEqualTo("map_point");
        assertThat(bindCaptor.getValue().getBizId()).isEqualTo(21L);
        verify(fileResourceService).publishForBusiness(7L, "map_point", 21L);
    }
}
