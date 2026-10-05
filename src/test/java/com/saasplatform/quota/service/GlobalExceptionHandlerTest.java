package com.saasplatform.quota.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.saasplatform.quota.TestData;
import com.saasplatform.quota.dto.ErrorResponse;
import com.saasplatform.quota.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    @Test
    void unexpectedExceptionsAreMaskedAsInternalError() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/boom");

        ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler(TestData.clock())
                .handleGeneric(new IllegalStateException("secret details"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain("secret");
        assertThat(response.getBody().path()).isEqualTo("/boom");
    }
}
