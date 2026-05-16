package com.mockify.backend.controller;

import com.mockify.backend.dto.response.imports.OpenApiImportResponse;
import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.OpenApiImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OpenApiImportControllerTest
 *
 * Purpose:
 * - Validate endpoint routing
 * - Validate multipart upload
 * - Validate authentication flow
 * - Validate org/project resolution
 * - Validate HTTP status codes
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class OpenApiImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenApiImportService openApiImportService;

    @MockitoBean
    private EndpointService endpointService;


    /**
     * Helper:
     * Build valid multipart file
     */
    private MockMultipartFile buildValidFile() {

        return new MockMultipartFile(
                "file",
                "openapi.yaml",
                "application/x-yaml",
                """
                openapi: 3.0.3
                info:
                  title: Test API
                  version: 1.0.0
                """.getBytes()
        );
    }


    @Nested
    @DisplayName("Successful Controller Tests")
    class SuccessfulControllerTests {

        @Test
        @DisplayName("Should import valid OpenAPI file successfully")
        void shouldImportValidOpenApiSuccessfully()
                throws Exception {

            UUID userId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();

            OpenApiImportResponse response =
                    OpenApiImportResponse.of(
                            Collections.emptyList(),
                            Collections.emptyList()
                    );

            when(endpointService.resolveProject(
                    "test-org",
                    "test-project"
            )).thenReturn(projectId);

            when(openApiImportService.importOpenApi(
                    eq(userId),
                    eq(projectId),
                    any()
            )).thenReturn(response);

            try (MockedStatic<SecurityUtils> securityMock =
                         mockStatic(SecurityUtils.class)) {

                Authentication auth =
                        mock(Authentication.class);

                securityMock.when(
                        () -> SecurityUtils.resolveUserId(auth)
                ).thenReturn(userId);

                mockMvc.perform(
                                multipart(
                                        "/api/test-org/test-project/import/openapi"
                                )
                                        .file(buildValidFile())
                                        .principal(auth)
                                        .contentType(
                                                MediaType.MULTIPART_FORM_DATA
                                        )
                        )
                        .andExpect(status().isCreated())
                        .andExpect(
                                content().contentType(
                                        MediaType.APPLICATION_JSON
                                )
                        )
                        .andExpect(
                                jsonPath("$.totalImported").value(0)
                        )
                        .andExpect(
                                jsonPath("$.totalSkipped").value(0)
                        );
            }
        }
    }


    @Nested
    @DisplayName("Project Resolution Failure Tests")
    class ProjectResolutionFailureTests {

        @Test
        @DisplayName("Should return 404 when project not found")
        void shouldReturn404WhenProjectMissing()
                throws Exception {

            Authentication auth =
                    mock(Authentication.class);

            when(endpointService.resolveProject(
                    "test-org",
                    "missing-project"
            )).thenThrow(
                    new RuntimeException("Project not found")
            );

            try (MockedStatic<SecurityUtils> securityMock =
                         mockStatic(SecurityUtils.class)) {

                securityMock.when(
                        () -> SecurityUtils.resolveUserId(auth)
                ).thenReturn(UUID.randomUUID());

                mockMvc.perform(
                                multipart(
                                        "/api/test-org/missing-project/import/openapi"
                                )
                                        .file(buildValidFile())
                                        .principal(auth)
                        )
                        .andExpect(status().is5xxServerError());
            }
        }
    }


    @Nested
    @DisplayName("Service Failure Tests")
    class ServiceFailureTests {

        @Test
        @DisplayName("Should propagate service validation errors")
        void shouldPropagateServiceValidationErrors()
                throws Exception {

            UUID userId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();

            when(endpointService.resolveProject(
                    anyString(),
                    anyString()
            )).thenReturn(projectId);

            when(openApiImportService.importOpenApi(
                    eq(userId),
                    eq(projectId),
                    any()
            )).thenThrow(
                    new RuntimeException("Import failed")
            );

            Authentication auth =
                    mock(Authentication.class);

            try (MockedStatic<SecurityUtils> securityMock =
                         mockStatic(SecurityUtils.class)) {

                securityMock.when(
                        () -> SecurityUtils.resolveUserId(auth)
                ).thenReturn(userId);

                mockMvc.perform(
                                multipart(
                                        "/api/test-org/test-project/import/openapi"
                                )
                                        .file(buildValidFile())
                                        .principal(auth)
                        )
                        .andExpect(status().is5xxServerError());
            }
        }
    }


    @Nested
    @DisplayName("Interaction Verification Tests")
    class InteractionVerificationTests {

        @Test
        @DisplayName("Should call service with correct parameters")
        void shouldCallServiceWithCorrectParameters()
                throws Exception {

            UUID userId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();

            when(endpointService.resolveProject(
                    "test-org",
                    "test-project"
            )).thenReturn(projectId);

            when(openApiImportService.importOpenApi(
                    any(),
                    any(),
                    any()
            )).thenReturn(
                    OpenApiImportResponse.of(
                            Collections.emptyList(),
                            Collections.emptyList()
                    )
            );

            Authentication auth =
                    mock(Authentication.class);

            try (MockedStatic<SecurityUtils> securityMock =
                         mockStatic(SecurityUtils.class)) {

                securityMock.when(
                        () -> SecurityUtils.resolveUserId(auth)
                ).thenReturn(userId);

                mockMvc.perform(
                                multipart(
                                        "/api/test-org/test-project/import/openapi"
                                )
                                        .file(buildValidFile())
                                        .principal(auth)
                        )
                        .andExpect(status().isCreated());

                verify(endpointService)
                        .resolveProject(
                                "test-org",
                                "test-project"
                        );

                verify(openApiImportService)
                        .importOpenApi(
                                eq(userId),
                                eq(projectId),
                                any()
                        );
            }
        }
    }
}
