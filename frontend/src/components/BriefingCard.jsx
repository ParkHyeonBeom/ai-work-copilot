import { useNavigate } from 'react-router-dom';

/** 브리핑 상태 레이블 */
const statusConfig = {
  COMPLETED: {
    label: '완료',
    className: 'bg-green-100 dark:bg-green-500/20 text-green-700 dark:text-green-400',
    icon: (
      <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" />
      </svg>
    ),
  },
  GENERATING: {
    label: '생성 중',
    className: 'bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-blue-400',
    icon: (
      <svg className="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
      </svg>
    ),
  },
  FAILED: {
    label: '실패',
    className: 'bg-red-100 dark:bg-red-500/20 text-red-700 dark:text-red-400',
    icon: (
      <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
      </svg>
    ),
  },
  PENDING: {
    label: '대기 중',
    className: 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400',
    icon: (
      <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
  },
};

/** 브리핑 카드 스켈레톤 */
export function BriefingCardSkeleton() {
  return (
    <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 p-5">
      <div className="flex items-start justify-between mb-3">
        <div className="skeleton h-5 w-32" />
        <div className="skeleton h-5 w-16 rounded-full" />
      </div>
      <div className="space-y-2 mb-4">
        <div className="skeleton h-4 w-full" />
        <div className="skeleton h-4 w-5/6" />
        <div className="skeleton h-4 w-3/4" />
      </div>
      <div className="skeleton h-3 w-24" />
    </div>
  );
}

export default function BriefingCard({
  briefing,
  isLoading = false,
  onGenerate,
  isGenerating = false,
  onRegenerate,
  isRegenerating = false,
}) {
  const navigate = useNavigate();

  // 브리핑이 없고 로딩도 아닌 경우: 생성 버튼
  if (!briefing && !isLoading) {
    return (
      <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 p-5">
        <div className="flex items-center gap-2 mb-3">
          <svg className="w-5 h-5 text-primary-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456z" />
          </svg>
          <h3 className="text-sm font-semibold text-gray-900 dark:text-white">
            오늘의 AI 브리핑
          </h3>
        </div>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
          아직 오늘의 브리핑이 생성되지 않았습니다. AI가 캘린더, 이메일, 드라이브를
          분석하여 오늘의 업무 브리핑을 준비합니다.
        </p>
        <button
          onClick={onGenerate}
          disabled={isGenerating}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-primary-500 hover:bg-primary-600 disabled:bg-primary-400 text-white text-sm font-medium rounded-lg transition-colors"
        >
          {isGenerating ? (
            <>
              <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
              브리핑 생성 중...
            </>
          ) : (
            <>
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
              </svg>
              브리핑 생성하기
            </>
          )}
        </button>
      </div>
    );
  }

  // 로딩 중
  if (isLoading) {
    return <BriefingCardSkeleton />;
  }

  // 브리핑 카드 표시
  const status = statusConfig[briefing.status] || statusConfig.PENDING;

  return (
    <div
      onClick={() => {
        if (briefing.id) {
          navigate(`/briefing/${briefing.id}`);
        }
      }}
      className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 p-5 cursor-pointer hover:border-primary-300 dark:hover:border-primary-700 hover:shadow-sm transition-all"
    >
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          <svg className="w-5 h-5 text-primary-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
          </svg>
          <h3 className="text-sm font-semibold text-gray-900 dark:text-white">
            오늘의 AI 브리핑
          </h3>
        </div>
        <span
          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${status.className}`}
        >
          {status.icon}
          {status.label}
        </span>
      </div>

      {/* Summary */}
      {briefing.summary && (
        <p className="text-sm text-gray-600 dark:text-gray-300 mb-4 line-clamp-3 leading-relaxed">
          {briefing.summary}
        </p>
      )}

      {/* Metadata */}
      <div className="flex items-center justify-between">
        <span className="text-xs text-gray-400 dark:text-gray-500">
          {briefing.createdAt &&
            new Date(briefing.createdAt).toLocaleTimeString('ko-KR', {
              hour: 'numeric',
              minute: '2-digit',
              hour12: true,
            })}{' '}
          생성
        </span>
        <div className="flex items-center gap-3">
          {onRegenerate && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onRegenerate();
              }}
              disabled={isRegenerating}
              className="inline-flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400 hover:text-primary-500 dark:hover:text-primary-400 font-medium transition-colors disabled:opacity-50"
            >
              {isRegenerating ? (
                <>
                  <svg className="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                  </svg>
                  재생성 중...
                </>
              ) : (
                <>
                  <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
                  </svg>
                  재생성
                </>
              )}
            </button>
          )}
          <span className="text-xs text-primary-500 dark:text-primary-400 font-medium">
            자세히 보기 &rarr;
          </span>
        </div>
      </div>
    </div>
  );
}
