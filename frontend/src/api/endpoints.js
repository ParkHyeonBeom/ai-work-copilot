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

  /** 사용자 검색 (이름 기반) */
  searchUsers: (query) => client.get('/users/search', { params: { q: query } }),
};

// ─── Calendar ──────────────────────────────────────────
export const calendar = {
  /** 오늘의 일정 조회 */
  getTodayEvents: () => client.get('/integrations/calendar/events/today'),

  /** 향후 N일간의 일정 조회 */
  getUpcomingEvents: (days = 7) =>
    client.get('/integrations/calendar/events', { params: { days } }),

  /** 범위별 일정 조회 (캘린더 뷰용) */
  getEventsByRange: (start, end) =>
    client.get('/integrations/calendar/events/range', { params: { start, end } }),

  /** 팀 일정 조회 (역할 기반 필터링) */
  getTeamEvents: (start, end) =>
    client.get('/integrations/calendar/events/team', { params: { start, end } }),

  /** 일정 생성 */
  createEvent: (data) => client.post('/integrations/calendar/events', data),

  /** 일정 수정 (사내 DB 이벤트만) */
  updateEvent: (eventId, data) => client.put(`/integrations/calendar/events/${eventId}`, data),

  /** 일정 삭제 (사내 DB 이벤트만) */
  deleteEvent: (eventId) => client.delete(`/integrations/calendar/events/${eventId}`),
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

// ─── Admin ──────────────────────────────────────────────
export const admin = {
  /** 승인 대기 유저 목록 */
  getPendingUsers: () => client.get('/admin/users/pending'),

  /** 유저 승인 (프로필 데이터 포함) */
  approveUser: (userId, profileData) => client.post(`/admin/users/${userId}/approve`, profileData),

  /** 유저 거부 */
  rejectUser: (userId) => client.post(`/admin/users/${userId}/reject`),
};

// ─── Notifications ─────────────────────────────────────
export const notifications = {
  /** 알림 목록 + 읽지 않은 수 조회 */
  getNotifications: () => client.get('/users/notifications'),

  /** 모두 읽음 처리 */
  markAllRead: () => client.post('/users/notifications/read'),
};

// ─── Briefings ─────────────────────────────────────────
export const briefings = {
  /** 일일 브리핑 생성 요청 */
  requestDailyBriefing: () => client.post('/briefings/daily'),

  /** 일일 브리핑 재생성 요청 (기존 브리핑 삭제 후 새로 생성) */
  regenerateDailyBriefing: () => client.post('/briefings/daily/regenerate'),

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

// ─── Chat ─────────────────────────────────────────────
export const chat = {
  /** 채팅방 생성 */
  createRoom: (data) => client.post('/chat/rooms', data),

  /** 내 채팅방 목록 */
  getRooms: () => client.get('/chat/rooms'),

  /** 채팅방 상세 */
  getRoom: (roomId) => client.get(`/chat/rooms/${roomId}`),

  /** 채팅방 나가기 */
  leaveRoom: (roomId) => client.delete(`/chat/rooms/${roomId}`),

  /** 멤버 초대 */
  inviteMembers: (roomId, memberIds) =>
    client.post(`/chat/rooms/${roomId}/invite`, { memberIds }),

  /** 메시지 히스토리 (cursor 페이지네이션) */
  getMessages: (roomId, cursor, size = 20) =>
    client.get(`/chat/rooms/${roomId}/messages`, { params: { cursor, size } }),

  /** 읽음 처리 */
  markAsRead: (roomId) => client.post(`/chat/rooms/${roomId}/read`),

  /** 전체 안읽은 메시지 수 */
  getUnreadCount: () => client.get('/chat/unread'),

  /** 파일 업로드 */
  uploadFile: (roomId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('roomId', roomId);
    return client.post('/chat/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  /** 파일 다운로드 URL */
  getFileDownloadUrl: (fileId) => `/api/chat/files/${fileId}/download`,
};

// ─── AI Agent ─────────────────────────────────────────
export const agent = {
  /** 메시지 전송 + AI 응답 */
  sendMessage: (data) => client.post('/ai/agent/chat', data),

  /** 대화 목록 */
  getConversations: () => client.get('/ai/agent/conversations'),

  /** 대화 상세 (히스토리) */
  getConversation: (id) => client.get(`/ai/agent/conversations/${id}`),

  /** 대화 삭제 */
  deleteConversation: (id) => client.delete(`/ai/agent/conversations/${id}`),
};
