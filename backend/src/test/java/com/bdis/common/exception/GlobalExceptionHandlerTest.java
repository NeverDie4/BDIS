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
}
