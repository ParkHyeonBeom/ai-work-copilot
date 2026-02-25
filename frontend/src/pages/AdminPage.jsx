import { useState, useEffect } from 'react';
import { admin } from '../api/endpoints';
import { useAuth } from '../contexts/AuthContext';
import { Navigate } from 'react-router-dom';

export default function AdminPage() {
  const { user } = useAuth();
  const [pendingUsers, setPendingUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(null);
  const [message, setMessage] = useState(null);
  const [expandedUser, setExpandedUser] = useState(null);
  const [profileForm, setProfileForm] = useState({ position: '', department: '', internalPhone: '' });

  // ADMIN이 아니면 대시보드로 리다이렉트
  if (user && user.role !== 'ADMIN') {
    return <Navigate to="/dashboard" replace />;
  }

  useEffect(() => {
    fetchPendingUsers();
  }, []);

  const fetchPendingUsers = async () => {
    try {
      const res = await admin.getPendingUsers();
      setPendingUsers(res.data.data || []);
    } catch (err) {
      console.error('승인 대기 목록 조회 실패:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleExpandApprove = (userId) => {
    if (expandedUser === userId) {
      setExpandedUser(null);
      setProfileForm({ position: '', department: '', internalPhone: '' });
    } else {
      setExpandedUser(userId);
      setProfileForm({ position: '', department: '', internalPhone: '' });
    }
  };

  const handleApprove = async (userId, userName) => {
    if (!profileForm.position || !profileForm.department) {
      setMessage({ type: 'error', text: '직급과 부서는 필수 입력입니다.' });
      return;
    }
    setActionLoading(userId);
    setMessage(null);
    try {
      await admin.approveUser(userId, profileForm);
      setMessage({ type: 'success', text: `${userName} 승인 완료. 인증 이메일이 발송되었습니다.` });
      setPendingUsers((prev) => prev.filter((u) => u.id !== userId));
      setExpandedUser(null);
      setProfileForm({ position: '', department: '', internalPhone: '' });
    } catch (err) {
      setMessage({ type: 'error', text: err.response?.data?.message || '승인 처리에 실패했습니다.' });
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async (userId, userName) => {
    if (!window.confirm(`${userName}의 가입을 거부하시겠습니까?`)) return;
    setActionLoading(userId);
    setMessage(null);
    try {
      await admin.rejectUser(userId);
      setMessage({ type: 'success', text: `${userName} 가입 거부 처리되었습니다.` });
      setPendingUsers((prev) => prev.filter((u) => u.id !== userId));
    } catch (err) {
      setMessage({ type: 'error', text: err.response?.data?.message || '거부 처리에 실패했습니다.' });
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="max-w-2xl mx-auto py-8 px-4">
      <div className="mb-6">
        <h1 className="text-xl font-bold text-gray-900 dark:text-white">
          사용자 관리
        </h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
          가입 승인 대기 중인 사용자를 관리합니다.
        </p>
      </div>

      {message && (
        <div
          className={`mb-4 px-4 py-3 rounded-lg text-sm ${
            message.type === 'success'
              ? 'bg-green-50 dark:bg-green-500/10 text-green-700 dark:text-green-400 border border-green-200 dark:border-green-500/20'
              : 'bg-red-50 dark:bg-red-500/10 text-red-700 dark:text-red-400 border border-red-200 dark:border-red-500/20'
          }`}
        >
          {message.text}
        </div>
      )}

      {loading ? (
        <div className="text-center py-12">
          <div className="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
          <p className="text-sm text-gray-500 dark:text-gray-400">로딩 중...</p>
        </div>
      ) : pendingUsers.length === 0 ? (
        <div className="text-center py-12 bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800">
          <svg className="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p className="text-sm text-gray-500 dark:text-gray-400">승인 대기 중인 사용자가 없습니다.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {pendingUsers.map((u) => (
            <div
              key={u.id}
              className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 overflow-hidden"
            >
              <div className="flex items-center justify-between p-4">
                <div className="flex items-center gap-3">
                  {u.profileImageUrl ? (
                    <img
                      src={u.profileImageUrl}
                      alt={u.name}
                      className="w-10 h-10 rounded-full"
                    />
                  ) : (
                    <div className="w-10 h-10 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
                      <span className="text-sm font-medium text-gray-500 dark:text-gray-400">
                        {u.name?.charAt(0) || '?'}
                      </span>
                    </div>
                  )}
                  <div>
                    <p className="text-sm font-medium text-gray-900 dark:text-white">{u.name}</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">{u.email}</p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleExpandApprove(u.id)}
                    disabled={actionLoading === u.id}
                    className="px-3 py-1.5 bg-primary-500 hover:bg-primary-600 disabled:bg-gray-300 dark:disabled:bg-gray-700 text-white text-xs font-medium rounded-lg transition-colors"
                  >
                    {expandedUser === u.id ? '취소' : '승인'}
                  </button>
                  <button
                    onClick={() => handleReject(u.id, u.name)}
                    disabled={actionLoading === u.id}
                    className="px-3 py-1.5 bg-red-500 hover:bg-red-600 disabled:bg-gray-300 dark:disabled:bg-gray-700 text-white text-xs font-medium rounded-lg transition-colors"
                  >
                    거부
                  </button>
                </div>
              </div>

              {/* 프로필 입력 폼 (인라인 확장) */}
              {expandedUser === u.id && (
                <div className="px-4 pb-4 border-t border-gray-100 dark:border-gray-800">
                  <p className="text-xs font-medium text-gray-500 dark:text-gray-400 mt-3 mb-3">
                    프로필 설정 (승인 시 필수)
                  </p>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div>
                      <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">직급 *</label>
                      <select
                        value={profileForm.position}
                        onChange={(e) => setProfileForm({ ...profileForm, position: e.target.value })}
                        className="w-full px-3 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                      >
                        <option value="">선택</option>
                        <option value="사원">사원</option>
                        <option value="대리">대리</option>
                        <option value="과장">과장</option>
                        <option value="차장">차장</option>
                        <option value="부장">부장</option>
                        <option value="이사">이사</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">부서 *</label>
                      <select
                        value={profileForm.department}
                        onChange={(e) => setProfileForm({ ...profileForm, department: e.target.value })}
                        className="w-full px-3 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                      >
                        <option value="">선택</option>
                        <option value="개발팀">개발팀</option>
                        <option value="기획팀">기획팀</option>
                        <option value="디자인팀">디자인팀</option>
                        <option value="마케팅팀">마케팅팀</option>
                        <option value="영업팀">영업팀</option>
                        <option value="인사팀">인사팀</option>
                        <option value="재무팀">재무팀</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">사내번호</label>
                      <input
                        type="text"
                        placeholder="예: 1234"
                        value={profileForm.internalPhone}
                        onChange={(e) => setProfileForm({ ...profileForm, internalPhone: e.target.value })}
                        className="w-full px-3 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                      />
                    </div>
                  </div>
                  <button
                    onClick={() => handleApprove(u.id, u.name)}
                    disabled={actionLoading === u.id || !profileForm.position || !profileForm.department}
                    className="mt-3 w-full px-4 py-2 bg-primary-500 hover:bg-primary-600 disabled:bg-gray-300 dark:disabled:bg-gray-700 text-white text-sm font-medium rounded-lg transition-colors"
                  >
                    {actionLoading === u.id ? '처리 중...' : '승인 확정'}
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
