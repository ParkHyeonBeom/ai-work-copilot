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
import com.workcopilot.integration.client.UserInfoClient;
import com.workcopilot.integration.client.UserNotificationClient;
import com.workcopilot.integration.dto.CalendarEventDto;
import com.workcopilot.integration.google.GoogleCredentialProvider;
import com.workcopilot.integration.repository.CalendarEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private GoogleCredentialProvider credentialProvider;

    @Mock
    private UserNotificationClient userNotificationClient;

    @Mock
    private CalendarEventRepository calendarEventRepository;

    @Mock
    private UserInfoClient userInfoClient;

    private CalendarService calendarService;

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
        calendarService = new CalendarService(credentialProvider, transport, jsonFactory, "AI Work Copilot Test", userNotificationClient, calendarEventRepository, userInfoClient);
    }

    @Test
    @DisplayName("getTodayEvents_GoogleAPI실패시_로컬이벤트만반환")
    void getTodayEvents_GoogleAPI실패시_로컬이벤트만반환() {
        // given
        given(credentialProvider.getCredential(USER_ID)).willReturn(createMockCredential());
        given(calendarEventRepository.findByCreatorUserIdAndStartTimeBetweenOrderByStartTimeAsc(any(), any(), any()))
                .willReturn(Collections.emptyList());

        // when
        List<CalendarEventDto> result = calendarService.getTodayEvents(USER_ID);

        // then
        assertThat(result).isEmpty();
        verify(credentialProvider).getCredential(USER_ID);
    }

    @Test
    @DisplayName("getUpcomingEvents_GoogleAPI실패시_로컬이벤트만반환")
    void getUpcomingEvents_GoogleAPI실패시_로컬이벤트만반환() {
        // given
        given(credentialProvider.getCredential(USER_ID)).willReturn(createMockCredential());
        given(calendarEventRepository.findByCreatorUserIdAndStartTimeBetweenOrderByStartTimeAsc(any(), any(), any()))
                .willReturn(Collections.emptyList());

        // when
        List<CalendarEventDto> result = calendarService.getUpcomingEvents(USER_ID, 7);

        // then
        assertThat(result).isEmpty();
        verify(credentialProvider).getCredential(USER_ID);
    }

    @Test
    @DisplayName("getEventById_유효한유저ID와이벤트ID_GoogleAPI호출시도")
    void getEventById_유효한유저ID와이벤트ID_GoogleAPI호출시도() {
        // given
        given(credentialProvider.getCredential(USER_ID)).willReturn(createMockCredential());

        // when & then
        assertThatThrownBy(() -> calendarService.getEventById(USER_ID, "event-1"))
                .isInstanceOf(BusinessException.class);

        verify(credentialProvider).getCredential(USER_ID);
    }

    @Test
    @DisplayName("getTodayEvents_토큰없는유저_BusinessException발생")
    void getTodayEvents_토큰없는유저_BusinessException발생() {
        // given
        given(credentialProvider.getCredential(anyLong()))
                .willThrow(new BusinessException(ErrorCode.TOKEN_REFRESH_FAILED));

        // when & then
        assertThatThrownBy(() -> calendarService.getTodayEvents(USER_ID))
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
