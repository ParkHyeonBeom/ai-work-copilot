package com.workcopilot.ai.service.prompt;

import com.workcopilot.ai.dto.BriefingRequest;
import com.workcopilot.ai.dto.CalendarEventDto;
import com.workcopilot.ai.dto.DriveFileDto;
import com.workcopilot.ai.dto.EmailDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BriefingPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            당신은 사내 업무 브리핑 AI입니다. 일정/이메일/파일을 분석하여 오늘의 핵심 업무를 파악하세요.

            규칙:
            - 파일명/이메일 제목을 그대로 나열하지 말고 의미를 해석하세요
            - 오늘 가장 중요한 업무 3가지를 우선순위로 파악하세요
            - 시간/마감이 있는 항목을 먼저 언급하세요
            - 한국어로 작성합니다

            좋은 예: summary: "오늘 3건의 회의와 KT&G 프로젝트 긴급 검토 필요"
            나쁜 예: summary: "이메일 및 드라이브의 최근 활동을 요약합니다"

            출력: 아래 JSON만 출력하세요.
            {"summary": "한줄 요약", "fullContent": "상세 브리핑", "keyPoints": ["핵심1", "핵심2"], "actionItems": ["할일1", "할일2"]}
            """;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String build(BriefingRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n\n");
        sb.append("=== 오늘 날짜: ").append(LocalDate.now().format(DATE_FORMATTER)).append(" ===\n\n");

        appendCalendarSection(sb, request.events());
        appendEmailSection(sb, request.emails());
        appendDriveSection(sb, request.files());

        sb.append("\n---\n");
        sb.append("[출력] JSON만 출력하세요:\n");
        sb.append("{\"summary\": \"...\", \"fullContent\": \"...\", \"keyPoints\": [...], \"actionItems\": [...]}\n");

        String prompt = sb.toString();
        log.debug("브리핑 프롬프트 생성 완료: events={}, emails={}, files={}",
                request.events() != null ? request.events().size() : 0,
                request.emails() != null ? request.emails().size() : 0,
                request.files() != null ? request.files().size() : 0);

        return prompt;
    }

    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    private void appendCalendarSection(StringBuilder sb, List<CalendarEventDto> events) {
        sb.append("## 오늘의 일정 (Google Calendar)\n");
        if (events == null || events.isEmpty()) {
            sb.append("- 오늘 등록된 일정이 없습니다.\n\n");
            return;
        }

        for (int i = 0; i < events.size(); i++) {
            CalendarEventDto event = events.get(i);
            sb.append(String.format("%d. **%s**\n", i + 1, event.title()));

            if (event.isAllDay()) {
                sb.append("   - 시간: 종일\n");
            } else {
                String startTime = event.startTime() != null ? event.startTime().format(TIME_FORMATTER) : "미정";
                String endTime = event.endTime() != null ? event.endTime().format(TIME_FORMATTER) : "미정";
                sb.append(String.format("   - 시간: %s ~ %s\n", startTime, endTime));
            }

            if (event.location() != null && !event.location().isBlank()) {
                sb.append(String.format("   - 장소: %s\n", event.location()));
            }
            if (event.attendees() != null && !event.attendees().isEmpty()) {
                sb.append(String.format("   - 참석자: %s\n", String.join(", ", event.attendees())));
            }
            if (event.description() != null && !event.description().isBlank()) {
                String desc = event.description().length() > 200
                        ? event.description().substring(0, 200) + "..."
                        : event.description();
                sb.append(String.format("   - 설명: %s\n", desc));
            }
        }
        sb.append("\n");
    }

    private void appendEmailSection(StringBuilder sb, List<EmailDto> emails) {
        sb.append("## 주요 이메일 (Gmail)\n");
        if (emails == null || emails.isEmpty()) {
            sb.append("- 새로운 이메일이 없습니다.\n\n");
            return;
        }

        // 중요 이메일을 먼저 표시
        List<EmailDto> sorted = emails.stream()
                .sorted((a, b) -> Boolean.compare(b.isImportant(), a.isImportant()))
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            EmailDto email = sorted.get(i);
            String importance = email.isImportant() ? " [중요]" : "";
            sb.append(String.format("%d. **%s**%s\n", i + 1, email.subject(), importance));
            sb.append(String.format("   - 보낸 사람: %s\n", email.from()));

            if (email.receivedAt() != null) {
                sb.append(String.format("   - 수신 시간: %s\n", email.receivedAt().format(TIME_FORMATTER)));
            }
            if (email.snippet() != null && !email.snippet().isBlank()) {
                String snippet = email.snippet().length() > 150
                        ? email.snippet().substring(0, 150) + "..."
                        : email.snippet();
                sb.append(String.format("   - 미리보기: %s\n", snippet));
            }
            if (email.labels() != null && !email.labels().isEmpty()) {
                sb.append(String.format("   - 라벨: %s\n", String.join(", ", email.labels())));
            }
        }
        sb.append("\n");
    }

    private void appendDriveSection(StringBuilder sb, List<DriveFileDto> files) {
        sb.append("## 최근 수정된 파일 (Google Drive)\n");
        if (files == null || files.isEmpty()) {
            sb.append("- 최근 수정된 파일이 없습니다.\n\n");
            return;
        }

        for (int i = 0; i < files.size(); i++) {
            DriveFileDto file = files.get(i);
            sb.append(String.format("%d. **%s**\n", i + 1, file.name()));
            sb.append(String.format("   - 유형: %s\n", file.mimeType()));

            if (file.modifiedTime() != null) {
                sb.append(String.format("   - 수정 시간: %s\n",
                        file.modifiedTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
            }
            if (file.owners() != null && !file.owners().isEmpty()) {
                sb.append(String.format("   - 소유자: %s\n", String.join(", ", file.owners())));
            }
        }
        sb.append("\n");
    }
}
