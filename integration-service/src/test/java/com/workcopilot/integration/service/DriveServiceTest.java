package com.workcopilot.integration.service;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.integration.google.GoogleCredentialProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DriveServiceTest {

    @Mock
    private GoogleCredentialProvider credentialProvider;

    private DriveService driveService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        MockLowLevelHttpResponse mockResponse = new MockLowLevelHttpResponse()
                .setStatusCode(403)
                .setContentType("application/json")
                .setContent("{\"error\":{\"code\":403,\"message\":\"Forbidden\",\"errors\":[{\"message\":\"Forbidden\",\"domain\":\"global\",\"reason\":\"forbidden\"}]}}");
        HttpTransport transport = new MockHttpTransport.Builder()
                .setLowLevelHttpResponse(mockResponse)
                .build();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        driveService = new DriveService(credentialProvider, transport, jsonFactory, "AI Work Copilot Test");
    }

    @Test
    @DisplayName("getRecentFiles_유효한유저ID_GoogleAPI호출시도")
    void getRecentFiles_유효한유저ID_GoogleAPI호출시도() {
        // given
        given(credentialProvider.getCredential(USER_ID)).willReturn(createMockCredential());

        // when & then
        assertThatThrownBy(() -> driveService.getRecentFiles(USER_ID, 10))
                .isInstanceOf(BusinessException.class);

        verify(credentialProvider).getCredential(USER_ID);
    }

    @Test
    @DisplayName("getFileContent_유효한파일ID_GoogleAPI호출시도")
    void getFileContent_유효한파일ID_GoogleAPI호출시도() {
        // given
        given(credentialProvider.getCredential(USER_ID)).willReturn(createMockCredential());

        // when & then
        assertThatThrownBy(() -> driveService.getFileContent(USER_ID, "file-123"))
                .isInstanceOf(BusinessException.class);

        verify(credentialProvider).getCredential(USER_ID);
    }

    @Test
    @DisplayName("getRecentFiles_토큰없는유저_BusinessException발생")
    void getRecentFiles_토큰없는유저_BusinessException발생() {
        // given
        given(credentialProvider.getCredential(anyLong()))
                .willThrow(new BusinessException(ErrorCode.TOKEN_REFRESH_FAILED));

        // when & then
        assertThatThrownBy(() -> driveService.getRecentFiles(USER_ID, 10))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getFileContent_토큰없는유저_BusinessException발생")
    void getFileContent_토큰없는유저_BusinessException발생() {
        // given
        given(credentialProvider.getCredential(anyLong()))
                .willThrow(new BusinessException(ErrorCode.TOKEN_REFRESH_FAILED));

        // when & then
        assertThatThrownBy(() -> driveService.getFileContent(USER_ID, "file-123"))
                .isInstanceOf(BusinessException.class);
    }

    private Credential createMockCredential() {
        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(new MockHttpTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .build();
        credential.setAccessToken("test-token");
        return credential;
    }
}
