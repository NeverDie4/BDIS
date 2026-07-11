package com.bdis.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.bdis.modules.audit.entity.DataSyncLogEntity;
import com.bdis.modules.audit.entity.FileAccessLogEntity;
import com.bdis.modules.audit.entity.OperationLogEntity;
import com.bdis.modules.soap.entity.SoapSyncTaskEntity;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

class EntityFieldMappingTest {

    @Test
    void normalizedEntityPropertiesShouldMapToDatabaseColumns() {
        assertMappings(
                OperationLogEntity.class,
                tuple("requestUrl", "request_url"),
                tuple("resultStatus", "result_status"));
        assertMappings(
                DataSyncLogEntity.class,
                tuple("sourceSystem", "source_system"),
                tuple("targetTable", "target_table"),
                tuple("errorMessage", "error_message"),
                tuple("bizType", "biz_type"),
                tuple("bizId", "biz_id"));
        assertMappings(FileAccessLogEntity.class, tuple("resultStatus", "result_status"));
        assertMappings(SoapSyncTaskEntity.class, tuple("isMock", "is_mock"));
    }

    private void assertMappings(Class<?> entityType, Tuple... expectedMappings) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(configuration, entityType.getName());
        assistant.setCurrentNamespace(entityType.getName());
        TableInfo tableInfo = TableInfoHelper.initTableInfo(assistant, entityType);

        assertThat(tableInfo.getFieldList())
                .extracting(TableFieldInfo::getProperty, TableFieldInfo::getColumn)
                .contains(expectedMappings);
    }
}
