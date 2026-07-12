package com.bdis.modules.declaration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bdis.modules.declaration.dto.DeclarationRequest;
import com.bdis.modules.declaration.dto.DeclarationReviewRequest;
import com.bdis.modules.declaration.entity.DeclarationEntity;
import com.bdis.modules.declaration.entity.DeclarationReviewRecordEntity;
import com.bdis.modules.declaration.query.DeclarationQuery;
import com.bdis.modules.declaration.vo.DeclarationDetailVO;
import com.bdis.modules.declaration.vo.DeclarationSummaryVO;

public interface DeclarationService {

    IPage<DeclarationEntity> listDeclarations(DeclarationQuery query);

    DeclarationEntity createDeclaration(DeclarationRequest request);

    DeclarationDetailVO getDeclarationDetail(Long declarationId);

    DeclarationEntity submitDeclaration(Long declarationId, Long applicantId);

    DeclarationReviewRecordEntity reviewDeclaration(
            Long declarationId, DeclarationReviewRequest request);

    DeclarationSummaryVO getDeclarationSummary(Long declarationId);
}
