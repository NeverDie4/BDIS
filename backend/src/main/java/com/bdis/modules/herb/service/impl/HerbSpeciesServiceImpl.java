package com.bdis.modules.herb.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.dictionary.support.DictionaryReferenceValidator;
import com.bdis.modules.herb.dto.HerbSpeciesCreateRequest;
import com.bdis.modules.herb.dto.HerbSpeciesQueryRequest;
import com.bdis.modules.herb.dto.HerbSpeciesUpdateRequest;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbSpeciesMapper;
import com.bdis.modules.herb.service.HerbSpeciesCoverFileService;
import com.bdis.modules.herb.service.HerbSpeciesService;
import com.bdis.modules.herb.vo.HerbSpeciesVO;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HerbSpeciesServiceImpl implements HerbSpeciesService {

    private final HerbSpeciesMapper herbSpeciesMapper;
    private final DictionaryReferenceValidator dictionaryReferenceValidator;
    private final HerbSpeciesCoverFileService coverFileService;

    public HerbSpeciesServiceImpl(
            HerbSpeciesMapper herbSpeciesMapper,
            DictionaryReferenceValidator dictionaryReferenceValidator,
            HerbSpeciesCoverFileService coverFileService) {
        this.herbSpeciesMapper = herbSpeciesMapper;
        this.dictionaryReferenceValidator = dictionaryReferenceValidator;
        this.coverFileService = coverFileService;
    }

    @Override
    @Transactional
    public HerbSpeciesVO create(HerbSpeciesCreateRequest request) {
        validateCreateRequest(request);
        dictionaryReferenceValidator.validateIfConfigured(
                "herb_category", request.getCategory(), "药材分类");
        if (herbSpeciesMapper.selectByHerbCode(request.getHerbCode()) != null) {
            throw new BusinessException("Herb code already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        HerbEntity entity = new HerbEntity();
        entity.setHerbNo(request.getHerbCode());
        entity.setHerbName(request.getHerbName());
        entity.setLatinName(request.getLatinName());
        entity.setAliasName(request.getAliasName());
        entity.setCategoryCode(request.getCategory());
        entity.setMedicinalPart(request.getMedicinalPart());
        entity.setEfficacy(request.getEfficacy());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(CurrentUserUtils.currentUserId());
        entity.setUpdatedBy(CurrentUserUtils.currentUserId());
        entity.setIsDeleted(0);
        herbSpeciesMapper.insertSpecies(entity);
        if (StringUtils.hasText(request.getCoverImageUrl())) {
            entity.setCoverImageUrl(
                    coverFileService.replaceCover(
                            entity.getId(), null, request.getCoverImageUrl()));
            entity.setUpdatedAt(LocalDateTime.now());
            herbSpeciesMapper.updateSpecies(entity);
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public HerbSpeciesVO update(Long id, HerbSpeciesUpdateRequest request) {
        HerbEntity existing = getActiveEntity(id);
        validateUpdateRequest(request);
        dictionaryReferenceValidator.validateIfConfigured(
                "herb_category", request.getCategory(), "药材分类");

        existing.setHerbName(request.getHerbName());
        existing.setLatinName(request.getLatinName());
        existing.setAliasName(request.getAliasName());
        existing.setCategoryCode(request.getCategory());
        existing.setMedicinalPart(request.getMedicinalPart());
        existing.setEfficacy(request.getEfficacy());
        existing.setDescription(request.getDescription());
        existing.setCoverImageUrl(
                coverFileService.replaceCover(
                        id, existing.getCoverImageUrl(), request.getCoverImageUrl()));
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(CurrentUserUtils.currentUserId());
        herbSpeciesMapper.updateSpecies(existing);
        return toVO(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        HerbEntity existing = getActiveEntity(id);
        coverFileService.deleteCover(id, existing.getCoverImageUrl());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(CurrentUserUtils.currentUserId());
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
        vo.setHerbCode(entity.getHerbNo());
        vo.setHerbName(entity.getHerbName());
        vo.setLatinName(entity.getLatinName());
        vo.setAliasName(entity.getAliasName());
        vo.setCategory(entity.getCategoryCode());
        vo.setCategoryName(displayCategoryName(entity));
        vo.setMedicinalPart(entity.getMedicinalPart());
        vo.setEfficacy(entity.getEfficacy());
        vo.setDescription(entity.getDescription());
        vo.setCoverImageUrl(entity.getCoverImageUrl());
        vo.setStatus(entity.getStatus());
        vo.setStatusText(statusText(entity.getStatus()));
        vo.setDistributionRegionText(entity.getDistributionRegionText());
        vo.setDistributionRegions(splitDistributionRegions(entity.getDistributionRegionText()));
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    private String displayCategoryName(HerbEntity entity) {
        if (StringUtils.hasText(entity.getCategoryName())) {
            return entity.getCategoryName();
        }
        return entity.getCategoryCode();
    }

    private String statusText(Integer status) {
        return Integer.valueOf(1).equals(status) ? "启用" : "停用";
    }

    private List<String> splitDistributionRegions(String distributionRegionText) {
        if (!StringUtils.hasText(distributionRegionText)) {
            return List.of();
        }
        return Arrays.stream(distributionRegionText.split("、"))
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
