import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { users } from '../api/endpoints';

const STEPS = [
  { id: 1, title: '캘린더 선택', description: '모니터링할 캘린더를 선택하세요' },
  { id: 2, title: '근무 시간', description: '일반적인 근무 시간을 설정하세요' },
  { id: 3, title: '중요 이메일', description: '중요 이메일 도메인을 설정하세요' },
  { id: 4, title: '시간대', description: '시간대를 선택하세요' },
];

const COMMON_DOMAINS = [
  'gmail.com',
  'naver.com',
  'daum.net',
  'kakao.com',
  'company.co.kr',
];

const TIMEZONES = [
  { value: 'Asia/Seoul', label: '대한민국 (UTC+9)' },
  { value: 'Asia/Tokyo', label: '일본 (UTC+9)' },
  { value: 'America/New_York', label: '미국 동부 (UTC-5)' },
  { value: 'America/Los_Angeles', label: '미국 서부 (UTC-8)' },
  { value: 'Europe/London', label: '영국 (UTC+0)' },
  { value: 'Europe/Berlin', label: '독일 (UTC+1)' },
  { value: 'Asia/Shanghai', label: '중국 (UTC+8)' },
  { value: 'Asia/Singapore', label: '싱가포르 (UTC+8)' },
];

export default function OnboardingPage() {
  const navigate = useNavigate();
  const { refreshUser } = useAuth();
  const [currentStep, setCurrentStep] = useState(1);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // Form data
  const [selectedCalendars, setSelectedCalendars] = useState(['primary']);
  const [workStartTime, setWorkStartTime] = useState('09:00');
  const [workEndTime, setWorkEndTime] = useState('18:00');
  const [importantDomains, setImportantDomains] = useState([]);
  const [customDomain, setCustomDomain] = useState('');
  const [timezone, setTimezone] = useState('Asia/Seoul');

  const handleNext = () => {
    if (currentStep < STEPS.length) {
      setCurrentStep(currentStep + 1);
    }
  };

  const handleBack = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
    }
  };

  const handleAddDomain = () => {
    const domain = customDomain.trim().toLowerCase();
    if (domain && !importantDomains.includes(domain)) {
      setImportantDomains([...importantDomains, domain]);
      setCustomDomain('');
    }
  };

  const handleRemoveDomain = (domain) => {
    setImportantDomains(importantDomains.filter((d) => d !== domain));
  };

  const handleTogglePresetDomain = (domain) => {
    if (importantDomains.includes(domain)) {
      handleRemoveDomain(domain);
    } else {
      setImportantDomains([...importantDomains, domain]);
    }
  };

  const handleSubmit = async () => {
    setIsSubmitting(true);
    setError(null);

    try {
      await users.completeOnboarding({
        selectedCalendars,
        workStartTime,
        workEndTime,
        importantDomains,
        timezone,
      });
      await refreshUser();
      navigate('/dashboard', { replace: true });
    } catch (err) {
      console.error('온보딩 완료 실패:', err);
      setError(
        err.response?.data?.message || '온보딩 처리 중 오류가 발생했습니다.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const renderStep = () => {
    switch (currentStep) {
      case 1:
        return (
          <div className="space-y-4">
            <div className="space-y-3">
              {['primary', 'work', 'personal'].map((cal) => {
                const labels = {
                  primary: { name: '기본 캘린더', desc: 'Google 기본 캘린더' },
                  work: { name: '업무 캘린더', desc: '업무 관련 일정' },
                  personal: { name: '개인 캘린더', desc: '개인 일정' },
                };
                const isSelected = selectedCalendars.includes(cal);
                return (
                  <button
                    key={cal}
                    onClick={() => {
                      if (isSelected) {
                        setSelectedCalendars(
                          selectedCalendars.filter((c) => c !== cal)
                        );
                      } else {
                        setSelectedCalendars([...selectedCalendars, cal]);
                      }
                    }}
                    className={`w-full flex items-center gap-3 p-4 rounded-xl border transition-all text-left ${
                      isSelected
                        ? 'border-primary-500 bg-primary-50 dark:bg-primary-500/10 dark:border-primary-500'
                        : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 rounded border-2 flex items-center justify-center transition-colors ${
                        isSelected
                          ? 'border-primary-500 bg-primary-500'
                          : 'border-gray-300 dark:border-gray-600'
                      }`}
                    >
                      {isSelected && (
                        <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                        </svg>
                      )}
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-900 dark:text-white">
                        {labels[cal].name}
                      </p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        {labels[cal].desc}
                      </p>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        );

      case 2:
        return (
          <div className="space-y-6">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  시작 시간
                </label>
                <input
                  type="time"
                  value={workStartTime}
                  onChange={(e) => setWorkStartTime(e.target.value)}
                  className="w-full px-4 py-3 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-xl text-sm text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  종료 시간
                </label>
                <input
                  type="time"
                  value={workEndTime}
                  onChange={(e) => setWorkEndTime(e.target.value)}
                  className="w-full px-4 py-3 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-xl text-sm text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                />
              </div>
            </div>
            <div className="bg-gray-50 dark:bg-gray-800/50 rounded-xl p-4">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                근무 시간:{' '}
                <span className="font-medium text-gray-900 dark:text-white">
                  {workStartTime} ~ {workEndTime}
                </span>
              </p>
              <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">
                브리핑은 근무 시작 30분 전에 자동으로 생성됩니다
              </p>
            </div>
          </div>
        );

      case 3:
        return (
          <div className="space-y-4">
            {/* Preset domains */}
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
                자주 사용되는 도메인
              </p>
              <div className="flex flex-wrap gap-2">
                {COMMON_DOMAINS.map((domain) => (
                  <button
                    key={domain}
                    onClick={() => handleTogglePresetDomain(domain)}
                    className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
                      importantDomains.includes(domain)
                        ? 'bg-primary-100 dark:bg-primary-500/20 text-primary-700 dark:text-primary-400 border border-primary-300 dark:border-primary-600'
                        : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 border border-gray-200 dark:border-gray-700 hover:border-gray-300'
                    }`}
                  >
                    @{domain}
                  </button>
                ))}
              </div>
            </div>

            {/* Custom domain input */}
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
                직접 추가
              </p>
              <div className="flex gap-2">
                <div className="flex-1 relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">
                    @
                  </span>
                  <input
                    type="text"
                    value={customDomain}
                    onChange={(e) => setCustomDomain(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleAddDomain()}
                    placeholder="example.com"
                    className="w-full pl-8 pr-4 py-2.5 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-xl text-sm text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                  />
                </div>
                <button
                  onClick={handleAddDomain}
                  className="px-4 py-2.5 bg-primary-500 hover:bg-primary-600 text-white text-sm font-medium rounded-xl transition-colors"
                >
                  추가
                </button>
              </div>
            </div>

            {/* Selected domains */}
            {importantDomains.length > 0 && (
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
                  선택된 도메인 ({importantDomains.length}개)
                </p>
                <div className="flex flex-wrap gap-2">
                  {importantDomains.map((domain) => (
                    <span
                      key={domain}
                      className="inline-flex items-center gap-1 px-3 py-1.5 bg-primary-50 dark:bg-primary-500/10 text-primary-700 dark:text-primary-400 rounded-full text-xs font-medium"
                    >
                      @{domain}
                      <button
                        onClick={() => handleRemoveDomain(domain)}
                        className="ml-0.5 hover:text-primary-900 dark:hover:text-primary-300"
                      >
                        <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        );

      case 4:
        return (
          <div className="space-y-3">
            {TIMEZONES.map((tz) => (
              <button
                key={tz.value}
                onClick={() => setTimezone(tz.value)}
                className={`w-full flex items-center justify-between p-4 rounded-xl border transition-all text-left ${
                  timezone === tz.value
                    ? 'border-primary-500 bg-primary-50 dark:bg-primary-500/10 dark:border-primary-500'
                    : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
                }`}
              >
                <span className="text-sm font-medium text-gray-900 dark:text-white">
                  {tz.label}
                </span>
                {timezone === tz.value && (
                  <svg className="w-5 h-5 text-primary-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                  </svg>
                )}
              </button>
            ))}
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex items-center justify-center p-4">
      <div className="w-full max-w-lg">
        {/* Progress bar */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-3">
            {STEPS.map((step) => (
              <div key={step.id} className="flex items-center">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium transition-colors ${
                    step.id <= currentStep
                      ? 'bg-primary-500 text-white'
                      : 'bg-gray-200 dark:bg-gray-700 text-gray-500 dark:text-gray-400'
                  }`}
                >
                  {step.id < currentStep ? (
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                    </svg>
                  ) : (
                    step.id
                  )}
                </div>
                {step.id < STEPS.length && (
                  <div
                    className={`hidden sm:block w-16 h-0.5 mx-2 ${
                      step.id < currentStep
                        ? 'bg-primary-500'
                        : 'bg-gray-200 dark:bg-gray-700'
                    }`}
                  />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Card */}
        <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 shadow-sm overflow-hidden">
          {/* Header */}
          <div className="px-6 py-5 border-b border-gray-100 dark:border-gray-800">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              {STEPS[currentStep - 1].title}
            </h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
              {STEPS[currentStep - 1].description}
            </p>
          </div>

          {/* Content */}
          <div className="px-6 py-6">{renderStep()}</div>

          {/* Error */}
          {error && (
            <div className="mx-6 mb-4 px-4 py-3 bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20 rounded-xl">
              <p className="text-sm text-red-600 dark:text-red-400">{error}</p>
            </div>
          )}

          {/* Footer */}
          <div className="px-6 py-4 border-t border-gray-100 dark:border-gray-800 flex items-center justify-between">
            <button
              onClick={handleBack}
              disabled={currentStep === 1}
              className="px-4 py-2 text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              이전
            </button>
            {currentStep < STEPS.length ? (
              <button
                onClick={handleNext}
                className="px-6 py-2.5 bg-primary-500 hover:bg-primary-600 text-white text-sm font-medium rounded-lg transition-colors"
              >
                다음
              </button>
            ) : (
              <button
                onClick={handleSubmit}
                disabled={isSubmitting}
                className="px-6 py-2.5 bg-primary-500 hover:bg-primary-600 disabled:bg-primary-400 text-white text-sm font-medium rounded-lg transition-colors inline-flex items-center gap-2"
              >
                {isSubmitting ? (
                  <>
                    <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    처리 중...
                  </>
                ) : (
                  '시작하기'
                )}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
