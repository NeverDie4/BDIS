package com.bdis.modules.declaration.vo;

import com.bdis.modules.declaration.entity.DeclarationArchiveEntity;
import com.bdis.modules.declaration.entity.DeclarationArchiveItemEntity;
import com.bdis.modules.declaration.entity.DeclarationEntity;
import com.bdis.modules.declaration.entity.DeclarationMaterialEntity;
import com.bdis.modules.declaration.entity.DeclarationReviewRecordEntity;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeclarationDetailVO {

    private DeclarationEntity declaration;

    private List<DeclarationMaterialEntity> materials;

    private List<DeclarationReviewRecordEntity> reviewRecords;

    private DeclarationArchiveEntity archive;

    private List<DeclarationArchiveItemEntity> archiveItems;
}
