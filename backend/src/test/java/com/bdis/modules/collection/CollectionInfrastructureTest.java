package com.bdis.modules.collection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CollectionInfrastructureTest {

    @Test
    void collectionInfrastructureClassesAndMapperXmlShouldExist() throws Exception {
        assertThat(Class.forName("com.bdis.modules.collection.entity.HerbCollectionTaskEntity"))
                .isNotNull();
        assertThat(Class.forName("com.bdis.modules.collection.entity.HerbBatchEntity"))
                .isNotNull();
        assertThat(Class.forName("com.bdis.modules.collection.entity.HerbBatchImageEntity"))
                .isNotNull();
        assertThat(Class.forName("com.bdis.modules.collection.mapper.HerbCollectionTaskMapper"))
                .isNotNull();
        assertThat(Class.forName("com.bdis.modules.collection.mapper.HerbBatchMapper"))
                .isNotNull();
        assertThat(Class.forName("com.bdis.modules.collection.mapper.HerbBatchImageMapper"))
                .isNotNull();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assertThat(classLoader.getResource("mapper/collection/HerbCollectionTaskMapper.xml"))
                .isNotNull();
        assertThat(classLoader.getResource("mapper/collection/HerbBatchMapper.xml")).isNotNull();
        assertThat(classLoader.getResource("mapper/collection/HerbBatchImageMapper.xml"))
                .isNotNull();
    }
}
