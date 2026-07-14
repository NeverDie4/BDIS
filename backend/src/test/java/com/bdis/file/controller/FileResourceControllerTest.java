package com.bdis.file.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.common.security.RequirePermission;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class FileResourceControllerTest {

    @Test
    void temporaryUploadCleanupUsesUploadPermission() throws Exception {
        Method method =
                FileResourceController.class.getMethod("deleteOwnUnboundUpload", Long.class);

        assertThat(method.getAnnotation(RequirePermission.class).value())
                .isEqualTo("file:resource:upload");
    }
}
