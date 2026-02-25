import client from './client';

// ─── Auth ──────────────────────────────────────────────
export const auth = {
  /** 현재 로그인한 사용자 정보 조회 */
  getMe: () => client.get('/users/me'),
};

// ─── Users ─────────────────────────────────────────────
export const users = {
  /** 사용자 설정 업데이트 */
  updateSettings: (data) => client.put('/users/me/settings', data),

  /** 온보딩 완료 */
  completeOnboarding: (data) => client.post('/users/me/onboarding', data),
};

// ─── Calendar ──────────────────────────────────────────
export const calendar = {
  /** 오늘의 일정 조회 */
  getTodayEvents: () => client.get('/integrations/calendar/events/today'),

  /** 향후 N일간의 일정 조회 */
  getUpcomingEvents: (days = 7) =>
    client.get('/integrations/calendar/events', { params: { days } }),
};

// ─── Gmail ─────────────────────────────────────────────
export const gmail = {
  /** 최근 이메일 조회 */
  getRecentEmails: (max = 20) =>
    client.get('/integrations/gmail/messages/recent', { params: { max } }),

  /** 중요 이메일 조회 */
  getImportantEmails: () => client.get('/integrations/gmail/messages/important'),
};

// ─── Drive ─────────────────────────────────────────────
export const drive = {
  /** 최근 파일 조회 */
  getRecentFiles: (max = 20) =>
    client.get('/integrations/drive/files', { params: { max } }),
};

// ─── Briefings ─────────────────────────────────────────
export const briefings = {
  /** 일일 브리핑 생성 요청 */
  requestDailyBriefing: () => client.post('/briefings/daily'),

  /** 브리핑 상세 조회 */
  getBriefing: (id) => client.get(`/briefings/${id}`),

  /** 브리핑 히스토리 조회 */
  getBriefingHistory: () => client.get('/briefings/history'),

  /** 브리핑 SSE 스트리밍 URL 반환 (EventSource에서 사용) */
  getStreamUrl: (id) => {
    const token = localStorage.getItem('accessToken');
    return `/api/briefings/${id}/stream?token=${encodeURIComponent(token)}`;
  },
};
