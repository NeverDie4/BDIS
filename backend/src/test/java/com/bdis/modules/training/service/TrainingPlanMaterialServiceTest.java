package com.bdis.modules.training.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bdis.audit.service.AuditLogService;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.training.entity.TrainingMaterialEntity;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.entity.TrainingPlanMaterialEntity;
import com.bdis.modules.training.mapper.TrainingMaterialMapper;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.mapper.TrainingPlanMaterialMapper;
import com.bdis.modules.training.request.TrainingPlanMaterialBindRequest;
import com.bdis.modules.training.service.impl.TrainingPlanMaterialServiceImpl;
import com.bdis.modules.training.vo.TrainingPlanMaterialVO;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TrainingPlanMaterialServiceTest {
    @Mock TrainingPlanMapper planMapper;
    @Mock TrainingMaterialMapper materialMapper;
    @Mock TrainingPlanMaterialMapper relationMapper;
    @Mock FileResourceMapper fileMapper;
    @Mock UserMapper userMapper;
    @Mock AuditLogService auditLogService;
    TrainingPlanMaterialService service;

    @BeforeEach void setUp(){
        service=new TrainingPlanMaterialServiceImpl(planMapper,materialMapper,relationMapper,fileMapper,userMapper,auditLogService);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("7","n/a"));
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}

    @Test void listReturnsJoinedRowsInMapperOrder(){
        TrainingPlanMaterialVO vo=new TrainingPlanMaterialVO();vo.setBindingId(1L);vo.setSortOrder(10);
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("draft"));
        when(relationMapper.selectListVO(1L)).thenReturn(List.of(vo));
        assertEquals(1,service.list(1L).size());
        verify(relationMapper).selectListVO(1L);
    }

    @Test void listRejectsMissingPlan(){
        assertEquals(ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class,()->service.list(1L)).getResultCode());
    }

    @Test void bindDefaultsAndAtomicallyIncrementsReuseCount(){
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("draft"));
        when(materialMapper.selectByIdIncludingDeleted(2L)).thenReturn(material(1));
        when(fileMapper.selectById(10L)).thenReturn(file());
        when(userMapper.selectById(7L)).thenReturn(user());
        when(relationMapper.insert(any(TrainingPlanMaterialEntity.class))).thenAnswer(inv->{((TrainingPlanMaterialEntity)inv.getArgument(0)).setId(3L);return 1;});
        when(materialMapper.incrementReuseCount(2L)).thenReturn(1);
        Long id=service.bind(1L,bind(null,null));
        assertEquals(3L,id);
        ArgumentCaptor<TrainingPlanMaterialEntity> c=ArgumentCaptor.forClass(TrainingPlanMaterialEntity.class);
        verify(relationMapper).insert(c.capture());
        assertEquals(1,c.getValue().getIsRequired());
        assertEquals(0,c.getValue().getSortOrder());
        verify(auditLogService).record(any());
    }

    @Test void bindRejectsDuplicateAndConvertsDatabaseUniqueConflict(){
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("draft"));
        when(materialMapper.selectByIdIncludingDeleted(2L)).thenReturn(material(1));
        when(fileMapper.selectById(10L)).thenReturn(file());
        when(relationMapper.selectByPlanAndMaterial(1L,2L)).thenReturn(new TrainingPlanMaterialEntity());
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class,()->service.bind(1L,bind(1,0))).getResultCode());
        when(relationMapper.selectByPlanAndMaterial(1L,2L)).thenReturn(null);
        when(userMapper.selectById(7L)).thenReturn(user());
        when(relationMapper.insert(any(TrainingPlanMaterialEntity.class))).thenThrow(new DuplicateKeyException("uk"));
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class,()->service.bind(1L,bind(1,0))).getResultCode());
        verify(materialMapper,never()).incrementReuseCount(anyLong());
    }

    @Test void bindRejectsPublishedClosedMissingOrDisabledMaterial(){
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        assertThrows(BusinessException.class,()->service.bind(1L,bind(1,0)));
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("closed"));
        assertThrows(BusinessException.class,()->service.bind(1L,bind(1,0)));
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("draft"));
        assertEquals(ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class,()->service.bind(1L,bind(1,0))).getResultCode());
        when(materialMapper.selectByIdIncludingDeleted(2L)).thenReturn(material(0));
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class,()->service.bind(1L,bind(1,0))).getResultCode());
        TrainingMaterialEntity deleted=material(1);deleted.setIsDeleted(1);
        when(materialMapper.selectByIdIncludingDeleted(2L)).thenReturn(deleted);
        assertEquals(ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class,()->service.bind(1L,bind(1,0))).getResultCode());
    }

    @Test void bindRollsBackWhenCounterUpdateFails(){
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("draft"));
        when(materialMapper.selectByIdIncludingDeleted(2L)).thenReturn(material(1));
        when(fileMapper.selectById(10L)).thenReturn(file());
        when(userMapper.selectById(7L)).thenReturn(user());
        when(relationMapper.insert(any(TrainingPlanMaterialEntity.class))).thenReturn(1);
        when(materialMapper.incrementReuseCount(2L)).thenReturn(0);
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class,()->service.bind(1L,bind(1,0))).getResultCode());
        verify(auditLogService,never()).record(any());
    }

    @Test void unbindDeletesOnlyRelationAndAtomicallyDecrements(){
        TrainingPlanMaterialEntity rel=new TrainingPlanMaterialEntity();rel.setId(3L);rel.setPlanId(1L);rel.setMaterialId(2L);
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("draft"));
        when(relationMapper.selectByPlanAndMaterial(1L,2L)).thenReturn(rel);
        when(userMapper.selectById(7L)).thenReturn(user());
        when(relationMapper.deleteById(3L)).thenReturn(1);
        when(materialMapper.decrementReuseCount(2L)).thenReturn(1);
        service.unbind(1L,2L);
        verify(relationMapper).deleteById(3L);
        verify(materialMapper).decrementReuseCount(2L);
        verify(materialMapper,never()).deleteById(anyLong());
        verify(auditLogService).record(any());
    }

    @Test void unbindRejectsMissingRelationAndCounterUnderflow(){
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("draft"));
        assertEquals(ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class,()->service.unbind(1L,2L)).getResultCode());
        TrainingPlanMaterialEntity rel=new TrainingPlanMaterialEntity();rel.setId(3L);
        when(relationMapper.selectByPlanAndMaterial(1L,2L)).thenReturn(rel);
        when(userMapper.selectById(7L)).thenReturn(user());
        when(relationMapper.deleteById(3L)).thenReturn(1);
        when(materialMapper.decrementReuseCount(2L)).thenReturn(0);
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class,()->service.unbind(1L,2L)).getResultCode());
        verify(auditLogService,never()).record(any());
    }

    @Test void hasValidMaterialDelegatesToJoinedValidityQuery(){
        when(relationMapper.countValidMaterialsForPublish(1L)).thenReturn(1L);
        assertTrue(service.hasValidMaterial(1L));
        when(relationMapper.countValidMaterialsForPublish(1L)).thenReturn(0L);
        assertFalse(service.hasValidMaterial(1L));
    }

    private TrainingPlanEntity plan(String status){
        TrainingPlanEntity p=new TrainingPlanEntity();p.setId(1L);p.setPublishStatus(status);
        p.setIsDeleted(0);p.setStatus(1);return p;
    }
    private TrainingMaterialEntity material(int status){
        TrainingMaterialEntity m=new TrainingMaterialEntity();m.setId(2L);m.setFileId(10L);m.setStatus(status);m.setIsDeleted(0);return m;
    }
    private TrainingPlanMaterialBindRequest bind(Integer required,Integer sort){
        TrainingPlanMaterialBindRequest r=new TrainingPlanMaterialBindRequest();r.setMaterialId(2L);
        r.setIsRequired(required);r.setSortOrder(sort);return r;
    }
    private UserEntity user(){
        UserEntity u=new UserEntity();u.setId(7L);u.setStatus(1);u.setIsDeleted(0);return u;
    }
    private FileResourceEntity file(){
        FileResourceEntity f=new FileResourceEntity();f.setId(10L);f.setStatus(1);f.setIsDeleted(0);return f;
    }
}
