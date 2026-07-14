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

    @Test
    void registersM12ToM14BusinessPolicies() {
        FileBusinessAccessPolicy coursePolicy = policy("edu_course");
        FileBusinessAccessPolicy projectPolicy = policy("research_project");
        FileBusinessAccessPolicy recordPolicy = policy("edu_experiment_record");
        FileBusinessPolicyRegistry registry =
                new FileBusinessPolicyRegistry(List.of(coursePolicy, projectPolicy, recordPolicy));

        assertThat(registry.can("edu_course", 1L, FileBusinessAction.VIEW)).isTrue();
        assertThat(registry.can("research_project", 1L, FileBusinessAction.VIEW)).isTrue();
        assertThat(registry.can("edu_experiment_record", 1L, FileBusinessAction.VIEW)).isTrue();
    }

    private FileBusinessAccessPolicy policy(String bizType) {
        return new FileBusinessAccessPolicy() {
            @Override
            public String bizType() {
                return bizType;
            }

            @Override
            public boolean exists(Long bizId) {
                return true;
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
                return true;
            }
        };
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
