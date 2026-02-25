import { useState, useEffect } from 'react';
import UserMentionInput from './UserMentionInput';

export default function EventCreateModal({ isOpen, onClose, onSubmit, initialDate }) {
  const [form, setForm] = useState({
    title: '',
    description: '',
    startTime: '',
    endTime: '',
    location: '',
    isAllDay: false,
  });
  const [selectedAttendees, setSelectedAttendees] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setForm({
        title: '',
        description: '',
        startTime: initialDate ? `${initialDate}T09:00` : '',
        endTime: initialDate ? `${initialDate}T10:00` : '',
        location: '',
        isAllDay: false,
      });
      setSelectedAttendees([]);
      setError('');
    }
  }, [isOpen, initialDate]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.title || !form.startTime || !form.endTime) return;

    setLoading(true);
    setError('');
    try {
      const startTime = form.isAllDay
        ? form.startTime.split('T')[0] + 'T00:00:00'
        : form.startTime + ':00';
      const endTime = form.isAllDay
        ? form.endTime.split('T')[0] + 'T23:59:59'
        : form.endTime + ':00';

      const payload = {
        title: form.title,
        description: form.description || null,
        startTime,
        endTime,
        location: form.location || null,
        attendeeEmails: selectedAttendees.map((u) => u.email),
        isAllDay: form.isAllDay,
      };
      await onSubmit(payload);
      onClose();
    } catch (err) {
      const message =
        err?.response?.data?.message || err?.message || '일정 생성에 실패했습니다.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-white dark:bg-gray-900 rounded-2xl shadow-xl w-full max-w-lg mx-4 overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-800">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">새 일정 만들기</h2>
          <button
            onClick={onClose}
            className="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded-lg"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-4 space-y-4">
          {error && (
            <div className="px-3 py-2 text-sm text-red-700 dark:text-red-400 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
              {error}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">제목 *</label>
            <input
              name="title"
              value={form.title}
              onChange={handleChange}
              required
              className="w-full px-3 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              placeholder="회의 제목"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">설명</label>
            <textarea
              name="description"
              value={form.description}
              onChange={handleChange}
              rows={2}
              className="w-full px-3 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
              placeholder="회의 내용 (선택)"
            />
          </div>

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              name="isAllDay"
              checked={form.isAllDay}
              onChange={handleChange}
              className="rounded border-gray-300 text-primary-500 focus:ring-primary-500"
            />
            <label className="text-sm text-gray-700 dark:text-gray-300">종일 일정</label>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">시작 *</label>
              <input
                type={form.isAllDay ? 'date' : 'datetime-local'}
                name="startTime"
                value={form.isAllDay ? form.startTime.split('T')[0] : form.startTime}
                onChange={handleChange}
                required
                className="w-full px-3 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">종료 *</label>
              <input
                type={form.isAllDay ? 'date' : 'datetime-local'}
                name="endTime"
                value={form.isAllDay ? form.endTime.split('T')[0] : form.endTime}
                onChange={handleChange}
                required
                className="w-full px-3 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">장소</label>
            <input
              name="location"
              value={form.location}
              onChange={handleChange}
              className="w-full px-3 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              placeholder="회의실 또는 링크"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">참석자</label>
            <UserMentionInput
              selectedUsers={selectedAttendees}
              onChange={setSelectedAttendees}
            />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={loading || !form.title}
              className="px-4 py-2 text-sm font-medium text-white bg-primary-500 hover:bg-primary-600 disabled:bg-gray-300 dark:disabled:bg-gray-700 rounded-lg transition-colors"
            >
              {loading ? '생성 중...' : '일정 생성'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
