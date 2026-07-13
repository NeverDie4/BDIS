package com.bdis.modules.map.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.security.CurrentUser;
import com.bdis.modules.herb.mapper.HerbMapper;
import com.bdis.modules.map.entity.MapPointEntity;
import com.bdis.modules.map.mapper.MapPointMapper;
import com.bdis.modules.map.query.MapPointQuery;
import com.bdis.modules.map.service.impl.MapPointServiceImpl;
import com.bdis.modules.map.vo.MapPointVO;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MapPointServiceImplTest {

    @Mock private MapPointMapper mapPointMapper;
    @Mock private HerbMapper herbMapper;
    @Mock private MapCoverFileService mapCoverFileService;

    private MapPointService service;

    @BeforeEach
    void setUp() {
        CurrentUser user =
                new CurrentUser(
                        10L,
                        "teacher",
                        "Teacher A",
                        null,
                        null,
                        Set.of("TEACHER"),
                        Set.of(2L),
                        Set.of("map:point:update"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        service = new MapPointServiceImpl(mapPointMapper, herbMapper, mapCoverFileService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listPassesIncludeDisabledFlagToMapper() {
        MapPointQuery query = new MapPointQuery();
        query.setIncludeDisabled(true);

        service.listMapPoints(query);

        verify(mapPointMapper).selectMapPoints(null, null, null, null, true);
    }

    @Test
    void updateStatusPersistsAndReturnsDisabledPoint() {
        MapPointEntity entity = new MapPointEntity();
        entity.setId(7L);
        entity.setStatus(1);
        MapPointVO result = new MapPointVO();
        result.setId(7L);
        result.setStatus(0);
        when(mapPointMapper.selectById(7L)).thenReturn(entity);
        when(mapPointMapper.selectMapPoints(null, null, null, null, true))
                .thenReturn(List.of(result));

        MapPointVO updated = service.updateMapPointStatus(7L, 0);

        assertThat(updated.getStatus()).isZero();
        assertThat(entity.getStatus()).isZero();
        verify(mapPointMapper).updateById(entity);
    }
}
