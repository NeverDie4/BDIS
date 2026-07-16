package com.bdis.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.common.core.Result;
import com.bdis.common.enums.ResultCodeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleNoResourceShouldReturnNotFound() {
        NoResourceFoundException exception =
                new NoResourceFoundException(HttpMethod.GET, "uploads/missing.txt");

        ResponseEntity<Result<Void>> response = exceptionHandler.handleNoResource(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResultCodeEnum.NOT_FOUND.getCode());
    }

    @Test
    void passwordVerificationShouldUseDedicatedCode() {
        ResponseEntity<Result<Void>> response =
                exceptionHandler.handleBusiness(new PasswordVerificationException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo(ResultCodeEnum.PASSWORD_VERIFICATION_FAILED.getCode());
    }

    @Test
    void preferenceConflictShouldUseDedicatedCode() {
        ResponseEntity<Result<Void>> response =
                exceptionHandler.handleBusiness(new ResourceConflictException("conflict"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo(ResultCodeEnum.RESOURCE_CONFLICT.getCode());
    }

    @Test
    void illegalArgumentShouldReturnValidationErrorMessage() {
        ResponseEntity<Result<Void>> response =
                exceptionHandler.handleIllegalArgument(
                        new IllegalArgumentException("来源类型和来源 ID 必须同时提供"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo(ResultCodeEnum.VALIDATION_ERROR.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("来源类型和来源 ID 必须同时提供");
    }
}
