package com.bdis.modules.collection.support;

import java.util.List;
import lombok.Getter;

@Getter
public class CollectionAccessScope {

    private final boolean allIncluded;

    private final List<Long> ownerIds;

    public CollectionAccessScope(boolean allIncluded, List<Long> ownerIds) {
        this.allIncluded = allIncluded;
        this.ownerIds = List.copyOf(ownerIds);
    }
}
