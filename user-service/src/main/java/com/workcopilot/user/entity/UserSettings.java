package com.workcopilot.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    private List<String> monitoredCalendarIds;
    private List<String> monitoredDriveFolderIds;
    private List<String> importantDomains;
    private List<String> excludeLabels;
    private String workStartTime;
    private String workEndTime;
    private String language;
    private String timezone;

    public static UserSettings defaults() {
        return UserSettings.builder()
                .monitoredCalendarIds(List.of("primary"))
                .monitoredDriveFolderIds(List.of("root"))
                .importantDomains(List.of())
                .excludeLabels(List.of("PROMOTIONS", "SOCIAL"))
                .workStartTime("09:00")
                .workEndTime("18:00")
                .language("ko")
                .timezone("Asia/Seoul")
                .build();
    }
}
