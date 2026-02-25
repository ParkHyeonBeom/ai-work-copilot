import { useState, useRef, useEffect, useCallback } from 'react';
import { users } from '../api/endpoints';

export default function UserMentionInput({ selectedUsers = [], onChange }) {
  const [query, setQuery] = useState('');
  const [allUsers, setAllUsers] = useState([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const [loading, setLoading] = useState(false);
  const [highlightIndex, setHighlightIndex] = useState(0);
  const inputRef = useRef(null);
  const dropdownRef = useRef(null);
  const loadedRef = useRef(false);

  const loadAllUsers = useCallback(async () => {
    if (loadedRef.current) return;
    setLoading(true);
    try {
      const res = await users.searchUsers('');
      setAllUsers(res.data.data || []);
      loadedRef.current = true;
    } catch {
      setAllUsers([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const filteredResults = allUsers
    .filter((u) => !selectedUsers.some((s) => s.email === u.email))
    .filter((u) =>
      !query || u.name.toLowerCase().includes(query.toLowerCase()) ||
      u.email.toLowerCase().includes(query.toLowerCase())
    );

  const handleFocus = async () => {
    await loadAllUsers();
    setShowDropdown(true);
    setHighlightIndex(0);
  };

  const handleInputChange = (e) => {
    const value = e.target.value;
    setQuery(value);
    setShowDropdown(true);
    setHighlightIndex(0);
  };

  const selectUser = (user) => {
    onChange([...selectedUsers, user]);
    setQuery('');
    setShowDropdown(false);
    inputRef.current?.focus();
  };

  const removeUser = (email) => {
    onChange(selectedUsers.filter((u) => u.email !== email));
  };

  const handleKeyDown = (e) => {
    if (!showDropdown || filteredResults.length === 0) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlightIndex((prev) => Math.min(prev + 1, filteredResults.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlightIndex((prev) => Math.max(prev - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (filteredResults[highlightIndex]) {
        selectUser(filteredResults[highlightIndex]);
      }
    } else if (e.key === 'Escape') {
      setShowDropdown(false);
    }
  };

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(e.target) &&
        inputRef.current &&
        !inputRef.current.contains(e.target)
      ) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="relative">
      {selectedUsers.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-2">
          {selectedUsers.map((user) => (
            <span
              key={user.email}
              className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 rounded-full"
            >
              {user.name}
              <button
                type="button"
                onClick={() => removeUser(user.email)}
                className="ml-0.5 hover:text-primary-900 dark:hover:text-primary-100"
              >
                <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </span>
          ))}
        </div>
      )}

      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={handleInputChange}
          onFocus={handleFocus}
          onKeyDown={handleKeyDown}
          placeholder="클릭하여 참석자 선택..."
          className="w-full px-3 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
        />
        {loading && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2">
            <div className="w-4 h-4 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
          </div>
        )}
      </div>

      {showDropdown && (
        <div
          ref={dropdownRef}
          className="absolute z-50 mt-1 w-full bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg max-h-48 overflow-y-auto"
        >
          {filteredResults.length === 0 ? (
            <div className="px-3 py-2 text-sm text-gray-500 dark:text-gray-400">
              {loading ? '불러오는 중...' : '검색 결과가 없습니다'}
            </div>
          ) : (
            filteredResults.map((user, idx) => (
              <button
                key={user.id}
                type="button"
                onClick={() => selectUser(user)}
                className={`w-full text-left px-3 py-2 text-sm transition-colors ${
                  idx === highlightIndex
                    ? 'bg-primary-50 dark:bg-primary-900/30'
                    : 'hover:bg-gray-50 dark:hover:bg-gray-700'
                }`}
              >
                <span className="font-medium text-gray-900 dark:text-white">{user.name}</span>
                <span className="text-gray-500 dark:text-gray-400 ml-1">({user.email})</span>
                {user.department && (
                  <span className="text-gray-400 dark:text-gray-500 ml-1">· {user.department}</span>
                )}
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}
