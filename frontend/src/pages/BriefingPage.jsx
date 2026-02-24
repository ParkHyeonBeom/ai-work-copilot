import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { briefings } from '../api/endpoints';

/** 간단한 마크다운 렌더러 (의존성 없이) */
function renderMarkdown(text) {
  if (!text) return '';

  let html = text
    // Escape HTML
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    // Headers
    .replace(/^### (.+)$/gm, '<h3>$1</h3>')
    .replace(/^## (.+)$/gm, '<h2>$1</h2>')
    .replace(/^# (.+)$/gm, '<h1>$1</h1>')
    // Bold and italic
    .replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    // Code blocks
    .replace(/```[\s\S]*?```/g, (match) => {
      const code = match.slice(3, -3).replace(/^\w*\n/, '');
      return `<pre><code>${code}</code></pre>`;
    })
    // Inline code
    .replace(/`(.+?)`/g, '<code>$1</code>')
    // Horizontal rule
    .replace(/^---$/gm, '<hr>')
    // Blockquote
    .replace(/^&gt; (.+)$/gm, '<blockquote>$1</blockquote>')
    // Unordered lists
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    // Links
    .replace(/\[(.+?)\]\((.+?)\)/g, '<a href="$2" target="_blank" rel="noopener" class="text-primary-500 hover:underline">$1</a>')
    // Line breaks (double newline → paragraph)
    .replace(/\n\n/g, '</p><p>')
    // Single line breaks
    .replace(/\n/g, '<br>');

  // Wrap li sequences in ul
  html = html.replace(/(<li>.*?<\/li>)+/gs, '<ul>$&</ul>');

  return `<p>${html}</p>`;
}

/** 날짜 포맷 */
function formatDate(isoString) {
  if (!isoString) return '';
  return new Date(isoString).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'short',
  });
}

function formatTime(isoString) {
  if (!isoString) return '';
  return new Date(isoString).toLocaleTimeString('ko-KR', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  });
}

/** 히스토리 아이템 스켈레톤 */
function HistorySkeleton() {
  return (
    <div className="px-4 py-3 space-y-1.5">
      <div className="skeleton h-4 w-3/4" />
      <div className="skeleton h-3 w-1/2" />
    </div>
  );
}

export default function BriefingPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [briefing, setBriefing] = useState(null);
  const [history, setHistory] = useState([]);
  const [streamContent, setStreamContent] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [isLoadingBriefing, setIsLoadingBriefing] = useState(true);
  const [isLoadingHistory, setIsLoadingHistory] = useState(true);
  const [error, setError] = useState(null);

  const contentRef = useRef(null);
  const eventSourceRef = useRef(null);

  /** 히스토리 목록 가져오기 */
  const fetchHistory = useCallback(async () => {
    setIsLoadingHistory(true);
    try {
      const res = await briefings.getBriefingHistory();
      setHistory(res.data.data || []);
    } catch (err) {
      console.error('히스토리 조회 실패:', err);
    } finally {
      setIsLoadingHistory(false);
    }
  }, []);

  /** 브리핑 상세 가져오기 */
  const fetchBriefing = useCallback(
    async (briefingId) => {
      if (!briefingId) return;
      setIsLoadingBriefing(true);
      setError(null);
      try {
        const res = await briefings.getBriefing(briefingId);
        const data = res.data.data;
        setBriefing(data);

        // 생성 중이면 스트리밍 시작
        if (data.status === 'GENERATING') {
          startStreaming(briefingId);
        }
      } catch (err) {
        console.error('브리핑 조회 실패:', err);
        setError('브리핑을 불러올 수 없습니다.');
      } finally {
        setIsLoadingBriefing(false);
      }
    },
    []
  );

  /** SSE 스트리밍 시작 */
  const startStreaming = useCallback((briefingId) => {
    // 기존 연결 종료
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    setIsStreaming(true);
    setStreamContent('');

    const url = briefings.getStreamUrl(briefingId);
    const es = new EventSource(url);
    eventSourceRef.current = es;

    es.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.content) {
          setStreamContent((prev) => prev + data.content);
        }
        if (data.done) {
          es.close();
          setIsStreaming(false);
          // 완료 후 브리핑 다시 가져오기
          fetchBriefing(briefingId);
        }
      } catch {
        // 일반 텍스트인 경우
        setStreamContent((prev) => prev + event.data);
      }
    };

    es.onerror = () => {
      es.close();
      setIsStreaming(false);
    };
  }, []);

  // 마운트 시 히스토리 가져오기
  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  // ID 변경 시 브리핑 가져오기
  useEffect(() => {
    if (id) {
      fetchBriefing(id);
    } else if (history.length > 0) {
      // ID 없으면 최신 브리핑으로 이동
      navigate(`/briefing/${history[0].id}`, { replace: true });
    }
  }, [id, fetchBriefing, history, navigate]);

  // 스트리밍 시 자동 스크롤
  useEffect(() => {
    if (contentRef.current && isStreaming) {
      contentRef.current.scrollTop = contentRef.current.scrollHeight;
    }
  }, [streamContent, isStreaming]);

  // Cleanup EventSource
  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, []);

  const displayContent = isStreaming
    ? streamContent
    : briefing?.content || '';

  return (
    <div className="max-w-7xl mx-auto">
      <div className="flex gap-6">
        {/* Sidebar: History */}
        <div className="hidden lg:block w-72 shrink-0">
          <div className="sticky top-24 bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 overflow-hidden">
            <div className="px-4 py-3 border-b border-gray-100 dark:border-gray-800">
              <h3 className="text-sm font-semibold text-gray-900 dark:text-white">
                브리핑 히스토리
              </h3>
            </div>
            <div className="max-h-[calc(100vh-200px)] overflow-y-auto">
              {isLoadingHistory ? (
                <>
                  <HistorySkeleton />
                  <HistorySkeleton />
                  <HistorySkeleton />
                </>
              ) : history.length === 0 ? (
                <div className="px-4 py-8 text-center">
                  <p className="text-sm text-gray-400 dark:text-gray-500">
                    브리핑 기록이 없습니다
                  </p>
                </div>
              ) : (
                <div className="divide-y divide-gray-100 dark:divide-gray-800">
                  {history.map((item) => (
                    <Link
                      key={item.id}
                      to={`/briefing/${item.id}`}
                      className={`block px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors ${
                        item.id === id
                          ? 'bg-primary-50 dark:bg-primary-500/10 border-l-2 border-primary-500'
                          : ''
                      }`}
                    >
                      <p
                        className={`text-sm truncate ${
                          item.id === id
                            ? 'font-semibold text-primary-600 dark:text-primary-400'
                            : 'font-medium text-gray-900 dark:text-white'
                        }`}
                      >
                        {formatDate(item.createdAt)}
                      </p>
                      <p className="text-xs text-gray-400 dark:text-gray-500 mt-0.5 truncate">
                        {item.summary || '브리핑 내용'}
                      </p>
                      <div className="flex items-center gap-1.5 mt-1">
                        <span
                          className={`w-1.5 h-1.5 rounded-full ${
                            item.status === 'COMPLETED'
                              ? 'bg-green-500'
                              : item.status === 'GENERATING'
                              ? 'bg-blue-500 animate-pulse'
                              : item.status === 'FAILED'
                              ? 'bg-red-500'
                              : 'bg-gray-400'
                          }`}
                        />
                        <span className="text-xs text-gray-400 dark:text-gray-500">
                          {formatTime(item.createdAt)}
                        </span>
                      </div>
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Main content */}
        <div className="flex-1 min-w-0">
          {error ? (
            <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 p-8 text-center">
              <div className="w-12 h-12 mx-auto mb-4 rounded-full bg-red-100 dark:bg-red-500/20 flex items-center justify-center">
                <svg className="w-6 h-6 text-red-600 dark:text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
                </svg>
              </div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                오류 발생
              </h3>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                {error}
              </p>
              <button
                onClick={() => navigate('/dashboard')}
                className="inline-flex items-center gap-2 px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white text-sm font-medium rounded-lg transition-colors"
              >
                대시보드로 이동
              </button>
            </div>
          ) : isLoadingBriefing && !briefing ? (
            <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 p-8">
              <div className="space-y-4">
                <div className="skeleton h-8 w-1/3" />
                <div className="skeleton h-4 w-1/4" />
                <div className="mt-8 space-y-3">
                  <div className="skeleton h-4 w-full" />
                  <div className="skeleton h-4 w-5/6" />
                  <div className="skeleton h-4 w-4/6" />
                  <div className="skeleton h-4 w-full" />
                  <div className="skeleton h-4 w-3/4" />
                </div>
              </div>
            </div>
          ) : !briefing && !isStreaming ? (
            <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 p-8 text-center">
              <svg className="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
              </svg>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                브리핑이 없습니다
              </h3>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                대시보드에서 새 브리핑을 생성해보세요
              </p>
              <button
                onClick={() => navigate('/dashboard')}
                className="inline-flex items-center gap-2 px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white text-sm font-medium rounded-lg transition-colors"
              >
                대시보드로 이동
              </button>
            </div>
          ) : (
            <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 overflow-hidden">
              {/* Header */}
              <div className="px-6 py-5 border-b border-gray-100 dark:border-gray-800">
                <div className="flex items-start justify-between">
                  <div>
                    <h1 className="text-xl font-bold text-gray-900 dark:text-white">
                      {briefing?.createdAt
                        ? formatDate(briefing.createdAt)
                        : '오늘'}{' '}
                      업무 브리핑
                    </h1>
                    <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                      {briefing?.createdAt && formatTime(briefing.createdAt)}{' '}
                      생성
                    </p>
                  </div>
                  {isStreaming && (
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-blue-400">
                      <span className="w-2 h-2 rounded-full bg-blue-500 animate-pulse" />
                      스트리밍 중
                    </span>
                  )}
                </div>
              </div>

              {/* Content */}
              <div
                ref={contentRef}
                className="px-6 py-6 max-h-[calc(100vh-280px)] overflow-y-auto"
              >
                <div
                  className="markdown-content text-gray-700 dark:text-gray-300"
                  dangerouslySetInnerHTML={{
                    __html: renderMarkdown(displayContent),
                  }}
                />
                {isStreaming && (
                  <span className="inline-block w-2 h-5 bg-primary-500 animate-pulse ml-0.5" />
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Mobile history (shown below content on small screens) */}
      <div className="lg:hidden mt-6">
        <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 overflow-hidden">
          <div className="px-4 py-3 border-b border-gray-100 dark:border-gray-800">
            <h3 className="text-sm font-semibold text-gray-900 dark:text-white">
              이전 브리핑
            </h3>
          </div>
          <div className="divide-y divide-gray-100 dark:divide-gray-800">
            {history.slice(0, 5).map((item) => (
              <Link
                key={item.id}
                to={`/briefing/${item.id}`}
                className={`block px-4 py-3 ${
                  item.id === id ? 'bg-primary-50 dark:bg-primary-500/10' : ''
                }`}
              >
                <p className="text-sm font-medium text-gray-900 dark:text-white">
                  {formatDate(item.createdAt)}
                </p>
                <p className="text-xs text-gray-400 dark:text-gray-500 mt-0.5 truncate">
                  {item.summary || '브리핑 내용'}
                </p>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
