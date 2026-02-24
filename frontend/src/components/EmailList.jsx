/**
 * 시간 포맷팅: 상대적 시간 표시
 */
function formatRelativeTime(isoString) {
  if (!isoString) return '';
  const date = new Date(isoString);
  const now = new Date();
  const diffMs = now - date;
  const diffMin = Math.floor(diffMs / 60000);
  const diffHour = Math.floor(diffMs / 3600000);
  const diffDay = Math.floor(diffMs / 86400000);

  if (diffMin < 1) return '방금 전';
  if (diffMin < 60) return `${diffMin}분 전`;
  if (diffHour < 24) return `${diffHour}시간 전`;
  if (diffDay < 7) return `${diffDay}일 전`;

  return date.toLocaleDateString('ko-KR', {
    month: 'short',
    day: 'numeric',
  });
}

/** 발신자 이름에서 이니셜 추출 */
function getInitials(name) {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
  return name.charAt(0).toUpperCase();
}

/** 아바타 배경색 (이름 기반) */
function getAvatarColor(name) {
  const colors = [
    'bg-blue-500',
    'bg-green-500',
    'bg-purple-500',
    'bg-orange-500',
    'bg-pink-500',
    'bg-teal-500',
    'bg-indigo-500',
    'bg-red-500',
  ];
  if (!name) return colors[0];
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
}

function EmailItem({ email }) {
  const isImportant = email.important || email.starred;
  const isUnread = !email.read;

  return (
    <div
      className={`flex items-start gap-3 px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors cursor-pointer ${
        isUnread ? 'bg-primary-50/50 dark:bg-primary-500/5' : ''
      }`}
    >
      {/* Avatar */}
      <div
        className={`shrink-0 w-9 h-9 rounded-full flex items-center justify-center text-white text-xs font-medium ${getAvatarColor(
          email.senderName
        )}`}
      >
        {getInitials(email.senderName)}
      </div>

      {/* Content */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <span
            className={`text-sm truncate ${
              isUnread
                ? 'font-semibold text-gray-900 dark:text-white'
                : 'font-medium text-gray-700 dark:text-gray-300'
            }`}
          >
            {email.senderName || email.senderEmail || '알 수 없음'}
          </span>
          {isImportant && (
            <span className="shrink-0 inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold bg-amber-100 dark:bg-amber-500/20 text-amber-700 dark:text-amber-400">
              중요
            </span>
          )}
          <span className="shrink-0 ml-auto text-xs text-gray-400 dark:text-gray-500">
            {formatRelativeTime(email.receivedAt)}
          </span>
        </div>
        <p
          className={`text-sm truncate mt-0.5 ${
            isUnread
              ? 'text-gray-900 dark:text-gray-200'
              : 'text-gray-600 dark:text-gray-400'
          }`}
        >
          {email.subject || '(제목 없음)'}
        </p>
        {email.snippet && (
          <p className="text-xs text-gray-400 dark:text-gray-500 truncate mt-0.5">
            {email.snippet}
          </p>
        )}
      </div>

      {/* Unread indicator */}
      {isUnread && (
        <div className="shrink-0 mt-2">
          <div className="w-2 h-2 rounded-full bg-primary-500" />
        </div>
      )}
    </div>
  );
}

/** 스켈레톤 로딩 */
function EmailSkeleton() {
  return (
    <div className="flex items-start gap-3 px-4 py-3">
      <div className="skeleton w-9 h-9 rounded-full shrink-0" />
      <div className="flex-1 space-y-2">
        <div className="flex items-center gap-2">
          <div className="skeleton h-4 w-24" />
          <div className="skeleton h-3 w-12 ml-auto" />
        </div>
        <div className="skeleton h-4 w-3/4" />
        <div className="skeleton h-3 w-full" />
      </div>
    </div>
  );
}

export default function EmailList({ emails = [], isLoading = false, maxItems = 5 }) {
  const displayEmails = emails.slice(0, maxItems);

  return (
    <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 overflow-hidden">
      <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100 dark:border-gray-800">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-white flex items-center gap-2">
          <svg className="w-4 h-4 text-primary-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75" />
          </svg>
          최근 이메일
        </h3>
        {emails.length > maxItems && (
          <span className="text-xs text-gray-400 dark:text-gray-500">
            +{emails.length - maxItems}건 더
          </span>
        )}
      </div>

      <div className="divide-y divide-gray-100 dark:divide-gray-800">
        {isLoading ? (
          <>
            <EmailSkeleton />
            <EmailSkeleton />
            <EmailSkeleton />
          </>
        ) : displayEmails.length === 0 ? (
          <div className="py-8 text-center">
            <svg className="w-10 h-10 mx-auto text-gray-300 dark:text-gray-600 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M21.75 9v.906a2.25 2.25 0 01-1.183 1.981l-6.478 3.488M2.25 9v.906a2.25 2.25 0 001.183 1.981l6.478 3.488m8.839 2.51l-4.66-2.51m0 0l-1.023-.55a2.25 2.25 0 00-2.134 0l-1.022.55m0 0l-4.661 2.51m16.5 1.615a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V8.844a2.25 2.25 0 011.183-1.98l7.5-4.04a2.25 2.25 0 012.134 0l7.5 4.04a2.25 2.25 0 011.183 1.98V19.5z" />
            </svg>
            <p className="text-sm text-gray-400 dark:text-gray-500">
              표시할 이메일이 없습니다
            </p>
          </div>
        ) : (
          displayEmails.map((email, index) => (
            <EmailItem key={email.id || index} email={email} />
          ))
        )}
      </div>
    </div>
  );
}
