package com.bdis.modules.declaration.service;

import com.bdis.modules.declaration.dto.DeclarationMaterialRequest;
import com.bdis.modules.declaration.entity.DeclarationMaterialEntity;

public interface DeclarationMaterialService {

    DeclarationMaterialEntity addMaterial(Long declarationId, DeclarationMaterialRequest request);
}
