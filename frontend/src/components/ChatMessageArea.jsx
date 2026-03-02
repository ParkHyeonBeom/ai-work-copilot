import { useEffect, useRef, useState } from 'react';
import { chat } from '../api/endpoints';

function DateSeparator({ date }) {
  const d = new Date(date);
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);

  const isToday = d.toDateString() === today.toDateString();
  const isYesterday = d.toDateString() === yesterday.toDateString();

  let label;
  if (isToday) {
    label = '오늘';
  } else if (isYesterday) {
    label = '어제';
  } else {
    label = d.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      weekday: 'long',
    });
  }

  return (
    <div className="flex items-center gap-3 my-4">
      <div className="flex-1 h-px bg-gray-200 dark:bg-gray-700" />
      <span className="text-[11px] text-gray-400 dark:text-gray-500 shrink-0">{label}</span>
      <div className="flex-1 h-px bg-gray-200 dark:bg-gray-700" />
    </div>
  );
}

function ImagePreview({ src, alt }) {
  const [showModal, setShowModal] = useState(false);

  return (
    <>
      <img
        src={src}
        alt={alt}
        className="max-w-[300px] max-h-[200px] rounded-lg cursor-pointer hover:opacity-90 transition-opacity"
        onClick={() => setShowModal(true)}
        loading="lazy"
      />
      {showModal && (
        <div
          className="fixed inset-0 z-[200] bg-black/80 flex items-center justify-center p-4"
          onClick={() => setShowModal(false)}
        >
          <img src={src} alt={alt} className="max-w-full max-h-full object-contain rounded-lg" />
          <button
            onClick={() => setShowModal(false)}
            className="absolute top-4 right-4 p-2 text-white/80 hover:text-white"
          >
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      )}
    </>
  );
}

function FileMessage({ content }) {
  try {
    const file = typeof content === 'string' ? JSON.parse(content) : content;
    const downloadUrl = chat.getFileDownloadUrl(file.fileId || file.id);
    const isImage = file.mimeType && file.mimeType.startsWith('image/');

    if (isImage) {
      return <ImagePreview src={downloadUrl} alt={file.originalFileName || '이미지'} />;
    }

    return (
      <a
        href={downloadUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="flex items-center gap-2 px-3 py-2 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
      >
        <svg className="w-5 h-5 text-gray-500 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m.75 12l3 3m0 0l3-3m-3 3v-6m-1.5-9H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
        </svg>
        <span className="text-xs truncate">{file.originalFileName || '파일'}</span>
      </a>
    );
  } catch {
    return <span className="text-xs text-gray-500">[파일]</span>;
  }
}

function ReplyBlock({ replyTo }) {
  if (!replyTo) return null;
  return (
    <div className="flex items-center gap-2 mb-1 px-2 py-1 border-l-2 border-primary-400 dark:border-primary-500 bg-gray-50 dark:bg-gray-700/50 rounded-r text-[11px]">
      <span className="font-medium text-primary-600 dark:text-primary-400">{replyTo.senderName}</span>
      <span className="text-gray-500 dark:text-gray-400 truncate">{replyTo.content || '메시지'}</span>
    </div>
  );
}

export default function ChatMessageArea({
  messages,
  currentUserId,
  loading,
  hasMore,
  onLoadMore,
  loadingMore,
  typingUsers,
  onReply,
  onDelete,
}) {
  const bottomRef = useRef(null);
  const containerRef = useRef(null);
  const prevHeightRef = useRef(0);
  const [contextMenu, setContextMenu] = useState(null);

  // Auto-scroll to bottom on new messages (only if near bottom)
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const isNearBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 150;
    if (isNearBottom) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  // Maintain scroll position after loading more
  useEffect(() => {
    if (!loadingMore && prevHeightRef.current > 0) {
      const container = containerRef.current;
      if (container) {
        const newHeight = container.scrollHeight;
        container.scrollTop = newHeight - prevHeightRef.current;
        prevHeightRef.current = 0;
      }
    }
  }, [loadingMore, messages]);

  const handleLoadMore = () => {
    const container = containerRef.current;
    if (container) {
      prevHeightRef.current = container.scrollHeight;
    }
    onLoadMore?.();
  };

  const handleContextMenu = (e, msg) => {
    if (msg.senderUserId !== currentUserId || msg.deleted) return;
    e.preventDefault();
    setContextMenu({ x: e.clientX, y: e.clientY, message: msg });
  };

  useEffect(() => {
    const close = () => setContextMenu(null);
    window.addEventListener('click', close);
    return () => window.removeEventListener('click', close);
  }, []);

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="w-6 h-6 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div ref={containerRef} className="flex-1 overflow-y-auto px-4 py-3 space-y-1">
      {/* Load More */}
      {hasMore && (
        <div className="flex justify-center py-2">
          <button
            onClick={handleLoadMore}
            disabled={loadingMore}
            className="text-xs text-primary-500 hover:text-primary-600 dark:text-primary-400 dark:hover:text-primary-300 px-3 py-1.5 rounded-full border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors disabled:opacity-50"
          >
            {loadingMore ? (
              <span className="flex items-center gap-1.5">
                <span className="w-3 h-3 border border-primary-500 border-t-transparent rounded-full animate-spin" />
                불러오는 중...
              </span>
            ) : (
              '이전 메시지 불러오기'
            )}
          </button>
        </div>
      )}

      {messages.length === 0 && (
        <div className="flex items-center justify-center h-full">
          <p className="text-sm text-gray-400 dark:text-gray-600">첫 메시지를 보내보세요</p>
        </div>
      )}

      {messages.map((msg, idx) => {
        const isMine = msg.senderUserId === currentUserId;
        const isSystem = msg.type === 'SYSTEM';

        // Date separator
        let showDateSeparator = false;
        if (idx === 0) {
          showDateSeparator = true;
        } else {
          const prevDate = new Date(messages[idx - 1].createdAt).toDateString();
          const curDate = new Date(msg.createdAt).toDateString();
          if (prevDate !== curDate) showDateSeparator = true;
        }

        if (isSystem) {
          return (
            <div key={msg.id || idx}>
              {showDateSeparator && msg.createdAt && <DateSeparator date={msg.createdAt} />}
              <div className="flex justify-center">
                <span className="text-[11px] text-gray-400 dark:text-gray-600 bg-gray-100 dark:bg-gray-800 px-3 py-1 rounded-full">
                  {msg.content}
                </span>
              </div>
            </div>
          );
        }

        return (
          <div key={msg.id || idx}>
            {showDateSeparator && msg.createdAt && <DateSeparator date={msg.createdAt} />}
            <div
              className={`flex ${isMine ? 'justify-end' : 'justify-start'} group mb-1`}
              onContextMenu={(e) => handleContextMenu(e, msg)}
            >
              <div className={`max-w-[70%] ${isMine ? 'order-2' : ''}`}>
                {!isMine && (
                  <p className="text-[11px] text-gray-500 dark:text-gray-400 mb-0.5 ml-1">
                    {msg.senderName || '알 수 없음'}
                  </p>
                )}

                {/* Reply block */}
                {msg.replyTo && <ReplyBlock replyTo={msg.replyTo} />}

                <div className="flex items-end gap-1">
                  {/* Action buttons (visible on hover) - for own messages on left */}
                  {isMine && !msg.deleted && (
                    <div className="flex gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
                      <button
                        onClick={() => onReply?.(msg)}
                        className="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded"
                        title="답장"
                      >
                        <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M9 15L3 9m0 0l6-6M3 9h12a6 6 0 010 12h-3" />
                        </svg>
                      </button>
                      <button
                        onClick={() => onDelete?.(msg.id)}
                        className="p-1 text-gray-400 hover:text-red-500 rounded"
                        title="삭제"
                      >
                        <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                        </svg>
                      </button>
                    </div>
                  )}

                  <div
                    className={`px-3 py-2 rounded-2xl text-sm break-words ${
                      msg.deleted
                        ? 'bg-gray-100 dark:bg-gray-800 text-gray-400 dark:text-gray-500 italic'
                        : isMine
                          ? 'bg-primary-500 text-white rounded-br-md'
                          : 'bg-white dark:bg-gray-800 text-gray-900 dark:text-white border border-gray-200 dark:border-gray-700 rounded-bl-md'
                    }`}
                  >
                    {msg.deleted ? (
                      <span className="text-xs">삭제된 메시지입니다</span>
                    ) : msg.type === 'FILE' ? (
                      <FileMessage content={msg.content} />
                    ) : (
                      <span className="whitespace-pre-wrap">{msg.content}</span>
                    )}
                  </div>

                  {/* Action buttons for others' messages */}
                  {!isMine && !msg.deleted && (
                    <div className="flex gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
                      <button
                        onClick={() => onReply?.(msg)}
                        className="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded"
                        title="답장"
                      >
                        <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M9 15L3 9m0 0l6-6M3 9h12a6 6 0 010 12h-3" />
                        </svg>
                      </button>
                    </div>
                  )}
                </div>

                <p className={`text-[10px] text-gray-400 dark:text-gray-600 mt-0.5 ${isMine ? 'text-right mr-1' : 'ml-1'}`}>
                  {msg.createdAt
                    ? new Date(msg.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
                    : ''}
                </p>
              </div>
            </div>
          </div>
        );
      })}

      {/* Typing indicator */}
      {typingUsers && typingUsers.length > 0 && (
        <div className="flex items-center gap-2 px-1 py-1">
          <div className="flex gap-1">
            <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
            <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
            <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
          </div>
          <span className="text-xs text-gray-400 dark:text-gray-500">
            {typingUsers.map((u) => u.userName).join(', ')}님이 입력 중...
          </span>
        </div>
      )}

      <div ref={bottomRef} />

      {/* Context menu */}
      {contextMenu && (
        <div
          className="fixed z-50 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg py-1 min-w-[120px]"
          style={{ top: contextMenu.y, left: contextMenu.x }}
        >
          <button
            onClick={() => { onReply?.(contextMenu.message); setContextMenu(null); }}
            className="w-full text-left px-3 py-1.5 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            답장
          </button>
          {contextMenu.message.senderUserId === currentUserId && (
            <button
              onClick={() => { onDelete?.(contextMenu.message.id); setContextMenu(null); }}
              className="w-full text-left px-3 py-1.5 text-sm text-red-500 hover:bg-gray-100 dark:hover:bg-gray-700"
            >
              삭제
            </button>
          )}
        </div>
      )}
    </div>
  );
}
