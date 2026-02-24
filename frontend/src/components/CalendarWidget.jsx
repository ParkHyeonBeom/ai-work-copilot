import { useMemo } from 'react';

/**
 * 시간 포맷팅: ISO 문자열 → "오후 2:00" 형식
 */
function formatTime(isoString) {
  if (!isoString) return '';
  const date = new Date(isoString);
  return date.toLocaleTimeString('ko-KR', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  });
}

/**
 * 시간 범위 계산 (분 단위)
 */
function getDurationMinutes(start, end) {
  if (!start || !end) return 0;
  return Math.round((new Date(end) - new Date(start)) / 60000);
}

/** 현재 시간을 기준으로 진행 중인지 확인 */
function isOngoing(start, end) {
  const now = new Date();
  return new Date(start) <= now && now <= new Date(end);
}

/** 이미 지난 이벤트인지 확인 */
function isPast(end) {
  return new Date(end) < new Date();
}

function EventItem({ event }) {
  const ongoing = isOngoing(event.startTime, event.endTime);
  const past = isPast(event.endTime);
  const duration = getDurationMinutes(event.startTime, event.endTime);

  return (
    <div
      className={`relative flex gap-4 py-3 ${
        past ? 'opacity-50' : ''
      }`}
    >
      {/* Time indicator dot */}
      <div className="flex flex-col items-center pt-1">
        <div
          className={`w-2.5 h-2.5 rounded-full ${
            ongoing
              ? 'bg-green-500 ring-4 ring-green-500/20'
              : past
              ? 'bg-gray-300 dark:bg-gray-600'
              : 'bg-primary-500'
          }`}
        />
        <div className="w-px flex-1 bg-gray-200 dark:bg-gray-700 mt-1" />
      </div>

      {/* Event content */}
      <div className="flex-1 min-w-0 pb-4">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <h4
              className={`text-sm font-medium truncate ${
                past
                  ? 'text-gray-500 dark:text-gray-500 line-through'
                  : 'text-gray-900 dark:text-white'
              }`}
            >
              {event.title || '(제목 없음)'}
            </h4>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
              {formatTime(event.startTime)} - {formatTime(event.endTime)}
              {duration > 0 && (
                <span className="ml-1.5 text-gray-400 dark:text-gray-500">
                  ({duration >= 60 ? `${Math.floor(duration / 60)}시간` : ''}{' '}
                  {duration % 60 > 0 ? `${duration % 60}분` : ''})
                </span>
              )}
            </p>
          </div>

          {ongoing && (
            <span className="shrink-0 inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 dark:bg-green-500/20 text-green-700 dark:text-green-400">
              <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse" />
              진행 중
            </span>
          )}
        </div>

        {/* Location */}
        {event.location && (
          <p className="text-xs text-gray-400 dark:text-gray-500 mt-1 flex items-center gap-1">
            <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 10.5a3 3 0 11-6 0 3 3 0 016 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1115 0z" />
            </svg>
            {event.location}
          </p>
        )}

        {/* Attendees */}
        {event.attendeesCount > 0 && (
          <p className="text-xs text-gray-400 dark:text-gray-500 mt-1 flex items-center gap-1">
            <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
            </svg>
            참석자 {event.attendeesCount}명
          </p>
        )}
      </div>
    </div>
  );
}

/** 스켈레톤 로딩 */
function EventSkeleton() {
  return (
    <div className="flex gap-4 py-3">
      <div className="flex flex-col items-center pt-1">
        <div className="w-2.5 h-2.5 rounded-full skeleton" />
        <div className="w-px flex-1 bg-gray-200 dark:bg-gray-700 mt-1" />
      </div>
      <div className="flex-1 pb-4 space-y-2">
        <div className="skeleton h-4 w-3/4" />
        <div className="skeleton h-3 w-1/2" />
      </div>
    </div>
  );
}

export default function CalendarWidget({ events = [], isLoading = false }) {
  const sortedEvents = useMemo(() => {
    return [...events].sort(
      (a, b) => new Date(a.startTime) - new Date(b.startTime)
    );
  }, [events]);

  return (
    <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-white flex items-center gap-2">
          <svg className="w-4 h-4 text-primary-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
          </svg>
          오늘의 일정
        </h3>
        <span className="text-xs text-gray-400 dark:text-gray-500">
          {new Date().toLocaleDateString('ko-KR', {
            month: 'long',
            day: 'numeric',
            weekday: 'short',
          })}
        </span>
      </div>

      <div className="divide-y-0">
        {isLoading ? (
          <>
            <EventSkeleton />
            <EventSkeleton />
            <EventSkeleton />
          </>
        ) : sortedEvents.length === 0 ? (
          <div className="py-8 text-center">
            <svg className="w-10 h-10 mx-auto text-gray-300 dark:text-gray-600 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5m-9-6h.008v.008H12v-.008zM12 15h.008v.008H12V15zm0 2.25h.008v.008H12v-.008zM9.75 15h.008v.008H9.75V15zm0 2.25h.008v.008H9.75v-.008zM7.5 15h.008v.008H7.5V15zm0 2.25h.008v.008H7.5v-.008zm6.75-4.5h.008v.008h-.008v-.008zm0 2.25h.008v.008h-.008V15zm0 2.25h.008v.008h-.008v-.008zm2.25-4.5h.008v.008H16.5v-.008zm0 2.25h.008v.008H16.5V15z" />
            </svg>
            <p className="text-sm text-gray-400 dark:text-gray-500">
              오늘 예정된 일정이 없습니다
            </p>
          </div>
        ) : (
          sortedEvents.map((event, index) => (
            <EventItem key={event.id || index} event={event} />
          ))
        )}
      </div>
    </div>
  );
}
