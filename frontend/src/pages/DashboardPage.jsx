import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { calendar, gmail, drive, briefings } from '../api/endpoints';
import BriefingCard from '../components/BriefingCard';
import CalendarWidget from '../components/CalendarWidget';
import EmailList from '../components/EmailList';

/** 시간 포맷팅 */
function formatRelativeTime(isoString) {
  if (!isoString) return '';
  const date = new Date(isoString);
  const now = new Date();
  const diffMs = now - date;
  const diffMin = Math.floor(diffMs / 60000);
  const diffHour = Math.floor(diffMs / 3600000);

  if (diffMin < 1) return '방금 전';
  if (diffMin < 60) return `${diffMin}분 전`;
  if (diffHour < 24) return `${diffHour}시간 전`;
  return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
}

/** Drive 파일 아이콘 */
function getFileIcon(mimeType) {
  if (!mimeType) return '📄';
  if (mimeType.includes('spreadsheet') || mimeType.includes('excel')) return '📊';
  if (mimeType.includes('presentation') || mimeType.includes('powerpoint')) return '📑';
  if (mimeType.includes('document') || mimeType.includes('word')) return '📝';
  if (mimeType.includes('pdf')) return '📕';
  if (mimeType.includes('image')) return '🖼️';
  if (mimeType.includes('folder')) return '📁';
  return '📄';
}

/** 파일 스켈레톤 */
function FileSkeleton() {
  return (
    <div className="flex items-center gap-3 px-4 py-3">
      <div className="skeleton w-8 h-8 rounded-lg shrink-0" />
      <div className="flex-1 space-y-1.5">
        <div className="skeleton h-4 w-3/4" />
        <div className="skeleton h-3 w-1/2" />
      </div>
    </div>
  );
}

const AUTO_REFRESH_INTERVAL = 5 * 60 * 1000; // 5 minutes

export default function DashboardPage() {
  const { user } = useAuth();

  const [todayBriefing, setTodayBriefing] = useState(null);
  const [events, setEvents] = useState([]);
  const [emails, setEmails] = useState([]);
  const [files, setFiles] = useState([]);

  const [loadingBriefing, setLoadingBriefing] = useState(true);
  const [loadingEvents, setLoadingEvents] = useState(true);
  const [loadingEmails, setLoadingEmails] = useState(true);
  const [loadingFiles, setLoadingFiles] = useState(true);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isRegenerating, setIsRegenerating] = useState(false);

  /** 데이터 가져오기 */
  const fetchData = useCallback(async () => {
    // Briefing
    const fetchBriefing = async () => {
      setLoadingBriefing(true);
      try {
        const res = await briefings.getBriefingHistory();
        const history = res.data.data || [];
        // 오늘 날짜의 브리핑 찾기
        const today = new Date().toISOString().slice(0, 10);
        const todayItem = history.find(
          (b) => b.createdAt && b.createdAt.startsWith(today)
        );
        setTodayBriefing(todayItem || null);
      } catch (err) {
        console.error('브리핑 조회 실패:', err);
      } finally {
        setLoadingBriefing(false);
      }
    };

    // Events
    const fetchEvents = async () => {
      setLoadingEvents(true);
      try {
        const res = await calendar.getTodayEvents();
        setEvents(res.data.data || []);
      } catch (err) {
        console.error('캘린더 조회 실패:', err);
      } finally {
        setLoadingEvents(false);
      }
    };

    // Emails
    const fetchEmails = async () => {
      setLoadingEmails(true);
      try {
        const res = await gmail.getImportantEmails();
        setEmails(res.data.data || []);
      } catch (err) {
        console.error('이메일 조회 실패:', err);
      } finally {
        setLoadingEmails(false);
      }
    };

    // Files
    const fetchFiles = async () => {
      setLoadingFiles(true);
      try {
        const res = await drive.getRecentFiles(10);
        setFiles(res.data.data || []);
      } catch (err) {
        console.error('드라이브 조회 실패:', err);
      } finally {
        setLoadingFiles(false);
      }
    };

    // 병렬로 모든 데이터 가져오기
    await Promise.allSettled([
      fetchBriefing(),
      fetchEvents(),
      fetchEmails(),
      fetchFiles(),
    ]);
  }, []);

  // 초기 로드 + 주기적 갱신
  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, AUTO_REFRESH_INTERVAL);
    return () => clearInterval(interval);
  }, [fetchData]);

  /** 브리핑 생성 */
  const handleGenerateBriefing = async () => {
    setIsGenerating(true);
    try {
      const res = await briefings.requestDailyBriefing();
      setTodayBriefing(res.data.data);
    } catch (err) {
      console.error('브리핑 생성 실패:', err);
    } finally {
      setIsGenerating(false);
    }
  };

  /** 브리핑 재생성 */
  const handleRegenerateBriefing = async () => {
    setIsRegenerating(true);
    try {
      const res = await briefings.regenerateDailyBriefing();
      setTodayBriefing(res.data.data);
    } catch (err) {
      console.error('브리핑 재생성 실패:', err);
    } finally {
      setIsRegenerating(false);
    }
  };

  const greeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return '좋은 아침이에요';
    if (hour < 18) return '좋은 오후에요';
    return '좋은 저녁이에요';
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Greeting */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          {greeting()}, {user?.name || '사용자'}님
        </h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
          {new Date().toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            weekday: 'long',
          })}
        </p>
      </div>

      {/* Top row: Briefing */}
      <BriefingCard
        briefing={todayBriefing}
        isLoading={loadingBriefing}
        onGenerate={handleGenerateBriefing}
        isGenerating={isGenerating}
        onRegenerate={handleRegenerateBriefing}
        isRegenerating={isRegenerating}
      />

      {/* Main grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Calendar - 2 cols */}
        <div className="lg:col-span-2">
          <CalendarWidget events={events} isLoading={loadingEvents} />
        </div>

        {/* Drive files - 1 col */}
        <div className="lg:col-span-1">
          <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 overflow-hidden">
            <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100 dark:border-gray-800">
              <h3 className="text-sm font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                <svg className="w-4 h-4 text-primary-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75m-8.69-6.44l-2.12-2.12a1.5 1.5 0 00-1.061-.44H4.5A2.25 2.25 0 002.25 6v12a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9a2.25 2.25 0 00-2.25-2.25h-5.379a1.5 1.5 0 01-1.06-.44z" />
                </svg>
                최근 파일
              </h3>
            </div>

            <div className="divide-y divide-gray-100 dark:divide-gray-800">
              {loadingFiles ? (
                <>
                  <FileSkeleton />
                  <FileSkeleton />
                  <FileSkeleton />
                  <FileSkeleton />
                </>
              ) : files.length === 0 ? (
                <div className="py-8 text-center">
                  <svg className="w-10 h-10 mx-auto text-gray-300 dark:text-gray-600 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75m-8.69-6.44l-2.12-2.12a1.5 1.5 0 00-1.061-.44H4.5A2.25 2.25 0 002.25 6v12a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9a2.25 2.25 0 00-2.25-2.25h-5.379a1.5 1.5 0 01-1.06-.44z" />
                  </svg>
                  <p className="text-sm text-gray-400 dark:text-gray-500">
                    최근 파일이 없습니다
                  </p>
                </div>
              ) : (
                files.map((file, index) => (
                  <a
                    key={file.id || index}
                    href={file.webViewLink || '#'}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-3 px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                  >
                    <div className="w-8 h-8 rounded-lg bg-gray-100 dark:bg-gray-800 flex items-center justify-center text-base shrink-0">
                      {getFileIcon(file.mimeType)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 dark:text-white truncate">
                        {file.name || '제목 없음'}
                      </p>
                      <p className="text-xs text-gray-400 dark:text-gray-500">
                        {file.modifiedBy && `${file.modifiedBy} · `}
                        {formatRelativeTime(file.modifiedAt)}
                      </p>
                    </div>
                  </a>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Email row */}
      <EmailList emails={emails} isLoading={loadingEmails} maxItems={6} />
    </div>
  );
}
