import { useState, useEffect } from 'react';
import UserMentionInput from './UserMentionInput';

export default function ChatCreateModal({ isOpen, onClose, onSubmit }) {
  const [form, setForm] = useState({ type: 'DIRECT', name: '' });
  const [selectedMembers, setSelectedMembers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setForm({ type: 'DIRECT', name: '' });
      setSelectedMembers([]);
      setError('');
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (selectedMembers.length === 0) {
      setError('참여자를 최소 1명 이상 선택하세요.');
      return;
    }
    if (form.type === 'DIRECT' && selectedMembers.length !== 1) {
      setError('1:1 채팅은 1명만 선택 가능합니다.');
      return;
    }
    if (form.type === 'GROUP' && !form.name.trim()) {
      setError('그룹 채팅방 이름을 입력하세요.');
      return;
    }

    setLoading(true);
    setError('');
    try {
      await onSubmit({
        type: form.type,
        name: form.type === 'DIRECT' ? null : form.name.trim(),
        memberIds: selectedMembers.map((u) => u.id),
      });
      onClose();
    } catch (err) {
      setError(err?.response?.data?.message || '채팅방 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-white dark:bg-gray-900 rounded-2xl shadow-xl w-full max-w-md mx-4 overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-800">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">새 채팅</h2>
          <button onClick={onClose} className="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded-lg">
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
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">채팅 유형</label>
            <div className="flex gap-2">
              {[
                { value: 'DIRECT', label: '1:1 채팅' },
                { value: 'GROUP', label: '그룹 채팅' },
              ].map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => setForm((prev) => ({ ...prev, type: opt.value }))}
                  className={`flex-1 px-3 py-2 text-sm font-medium rounded-lg border transition-colors ${
                    form.type === opt.value
                      ? 'bg-primary-50 dark:bg-primary-500/10 border-primary-300 dark:border-primary-500/30 text-primary-600 dark:text-primary-400'
                      : 'bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {form.type === 'GROUP' && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">방 이름 *</label>
              <input
                value={form.name}
                onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
                className="w-full px-3 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                placeholder="채팅방 이름"
              />
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              참여자 {form.type === 'DIRECT' ? '(1명)' : ''}
            </label>
            <UserMentionInput
              selectedUsers={selectedMembers}
              onChange={setSelectedMembers}
            />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors">
              취소
            </button>
            <button type="submit" disabled={loading} className="px-4 py-2 text-sm font-medium text-white bg-primary-500 hover:bg-primary-600 disabled:bg-gray-300 dark:disabled:bg-gray-700 rounded-lg transition-colors">
              {loading ? '생성 중...' : '채팅 시작'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
