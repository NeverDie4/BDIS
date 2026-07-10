package com.bdis.modules.herb.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.herb.dto.HerbSpeciesCreateRequest;
import com.bdis.modules.herb.dto.HerbSpeciesQueryRequest;
import com.bdis.modules.herb.dto.HerbSpeciesUpdateRequest;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.herb.service.HerbSpeciesService;
import com.bdis.modules.herb.vo.HerbSpeciesVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HerbSpeciesServiceImpl implements HerbSpeciesService {

    private final HerbSpeciesMapper herbSpeciesMapper;

    public HerbSpeciesServiceImpl(HerbSpeciesMapper herbSpeciesMapper) {
        this.herbSpeciesMapper = herbSpeciesMapper;
    }

    @Override
    @Transactional
    public HerbSpeciesVO create(HerbSpeciesCreateRequest request) {
        validateCreateRequest(request);
        if (herbSpeciesMapper.selectByHerbCode(request.getHerbCode()) != null) {
            throw new BusinessException("Herb code already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        HerbEntity entity = new HerbEntity();
        entity.setHerbCode(request.getHerbCode());
        entity.setHerbName(request.getHerbName());
        entity.setLatinName(request.getLatinName());
        entity.setAliasName(request.getAliasName());
        entity.setCategory(request.getCategory());
        entity.setMedicinalPart(request.getMedicinalPart());
        entity.setEfficacy(request.getEfficacy());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setDeleted(0);
        herbSpeciesMapper.insertSpecies(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public HerbSpeciesVO update(Long id, HerbSpeciesUpdateRequest request) {
        HerbEntity existing = getActiveEntity(id);
        validateUpdateRequest(request);

        existing.setHerbName(request.getHerbName());
        existing.setLatinName(request.getLatinName());
        existing.setAliasName(request.getAliasName());
        existing.setCategory(request.getCategory());
        existing.setMedicinalPart(request.getMedicinalPart());
        existing.setEfficacy(request.getEfficacy());
        existing.setDescription(request.getDescription());
        existing.setStatus(request.getStatus());
        existing.setUpdateTime(LocalDateTime.now());
        herbSpeciesMapper.updateSpecies(existing);
        return toVO(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        HerbEntity existing = getActiveEntity(id);
        existing.setUpdateTime(LocalDateTime.now());
        int affected = herbSpeciesMapper.logicalDeleteById(existing);
        if (affected == 0) {
            throw new BusinessException("Herb species not found or already deleted");
        }
    }

    @Override
    public HerbSpeciesVO getById(Long id) {
        return toVO(getActiveEntity(id));
    }

    @Override
    public PageResult<HerbSpeciesVO> page(HerbSpeciesQueryRequest request) {
        normalizePageRequest(request);
        Long total = herbSpeciesMapper.countPage(request);
        Long offset = (long) (request.getPageNum() - 1) * request.getPageSize();
        List<HerbSpeciesVO> records =
                herbSpeciesMapper.selectPage(request, offset, request.getPageSize()).stream()
                        .map(this::toVO)
                        .toList();
        return new PageResult<>(total, request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    public List<HerbSpeciesVO> listEnabled() {
        return herbSpeciesMapper.selectEnabledList().stream().map(this::toVO).toList();
    }

    private HerbEntity getActiveEntity(Long id) {
        if (id == null) {
            throw new BusinessException("Herb species id is required");
        }
        HerbEntity entity = herbSpeciesMapper.selectActiveById(id);
        if (entity == null) {
            throw new BusinessException("Herb species not found");
        }
        return entity;
    }

    private void validateCreateRequest(HerbSpeciesCreateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getHerbCode())
                || !StringUtils.hasText(request.getHerbName())) {
            throw new BusinessException("Herb code and herb name are required");
        }
    }

    private void validateUpdateRequest(HerbSpeciesUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getHerbName())) {
            throw new BusinessException("Herb name is required");
        }
    }

    private void normalizePageRequest(HerbSpeciesQueryRequest request) {
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
    }

    private HerbSpeciesVO toVO(HerbEntity entity) {
        HerbSpeciesVO vo = new HerbSpeciesVO();
        vo.setId(entity.getId());
        vo.setHerbCode(entity.getHerbCode());
        vo.setHerbName(entity.getHerbName());
        vo.setLatinName(entity.getLatinName());
        vo.setAliasName(entity.getAliasName());
        vo.setCategory(entity.getCategory());
        vo.setMedicinalPart(entity.getMedicinalPart());
        vo.setEfficacy(entity.getEfficacy());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
