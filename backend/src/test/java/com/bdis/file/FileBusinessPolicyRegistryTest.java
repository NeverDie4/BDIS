package com.bdis.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.file.policy.FileBusinessAccessPolicy;
import com.bdis.file.policy.FileBusinessAction;
import com.bdis.file.policy.FileBusinessPolicyRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class FileBusinessPolicyRegistryTest {

    @Test
    void unregisteredBusinessTypeIsDeniedByDefault() {
        FileBusinessPolicyRegistry registry = new FileBusinessPolicyRegistry(List.of());

        assertThatThrownBy(() -> registry.require("future_business", 1L, FileBusinessAction.VIEW))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("未注册");
        assertThat(registry.can("future_business", 1L, FileBusinessAction.VIEW)).isFalse();
    }

    @Test
    void publishUsesDedicatedPolicyDecision() {
        FileBusinessPolicyRegistry registry =
                new FileBusinessPolicyRegistry(List.of(new TestPolicy()));

        assertThat(registry.can("test_business", 1L, FileBusinessAction.VIEW)).isTrue();
        assertThat(registry.can("test_business", 1L, FileBusinessAction.PUBLISH)).isFalse();
        assertThatThrownBy(() -> registry.require("test_business", 1L, FileBusinessAction.PUBLISH))
                .isInstanceOf(ForbiddenException.class);
    }

    private static final class TestPolicy implements FileBusinessAccessPolicy {

        @Override
        public String bizType() {
            return "test_business";
        }

        @Override
        public boolean exists(Long bizId) {
            return bizId == 1L;
        }

        @Override
        public boolean canView(Long bizId) {
            return true;
        }

        @Override
        public boolean canAttach(Long bizId) {
            return true;
        }

        @Override
        public boolean canDetach(Long bizId) {
            return true;
        }

        @Override
        public boolean canPublish(Long bizId) {
            return false;
        }
    }
}
