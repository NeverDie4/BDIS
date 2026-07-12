package com.bdis.modules.declaration.service;

import com.bdis.modules.declaration.dto.DeclarationArchiveItemRequest;
import com.bdis.modules.declaration.entity.DeclarationArchiveEntity;
import com.bdis.modules.declaration.entity.DeclarationArchiveItemEntity;

public interface DeclarationArchiveService {

    DeclarationArchiveEntity generateArchive(Long declarationId);

    DeclarationArchiveItemEntity addArchiveItem(
            Long archiveId, DeclarationArchiveItemRequest request);
}
