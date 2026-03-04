import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { chat } from '../api/endpoints';
import { useAuth } from '../contexts/AuthContext';
import useWebSocket from '../hooks/useWebSocket';
import ChatRoomList from '../components/ChatRoomList';
import ChatMessageArea from '../components/ChatMessageArea';
import ChatInput from '../components/ChatInput';
import ChatCreateModal from '../components/ChatCreateModal';

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const ALLOWED_MIME_PREFIXES = ['image/', 'application/pdf', 'text/', 'application/zip', 'application/x-zip',
  'application/msword', 'application/vnd.openxmlformats', 'application/vnd.ms-excel', 'application/vnd.ms-powerpoint'];

export default function ChatPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { connected, subscribe, unsubscribe, publish, addNotificationListener, addPresenceListener } = useWebSocket();

  const [rooms, setRooms] = useState([]);
  const [currentRoom, setCurrentRoom] = useState(null);
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [typingUsers, setTypingUsers] = useState([]);
  const [replyTo, setReplyTo] = useState(null);
  const [onlineUsers, setOnlineUsers] = useState(new Set());
  const [isDragging, setIsDragging] = useState(false);
  const [searchMode, setSearchMode] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const typingTimeoutsRef = useRef(new Map());
  const dragCounterRef = useRef(0);
  const PAGE_SIZE = 20;

  // Fetch rooms
  const fetchRooms = useCallback(async () => {
    try {
      const res = await chat.getRooms();
      setRooms(res.data.data || []);
    } catch (err) {
      console.error('채팅방 목록 조회 실패:', err);
    }
  }, []);

  useEffect(() => {
    fetchRooms();
  }, [fetchRooms]);

  // Fetch initial online users
  useEffect(() => {
    chat.getOnlineUsers?.()?.then((res) => {
      setOnlineUsers(new Set(res.data.data || []));
    }).catch(() => {});
  }, []);

  // Listen for presence updates
  useEffect(() => {
    if (!addPresenceListener) return;
    const removeListener = addPresenceListener((event) => {
      if (event.type === 'PRESENCE') {
        setOnlineUsers((prev) => {
          const next = new Set(prev);
          if (event.online) next.add(event.userId);
          else next.delete(event.userId);
          return next;
        });
      }
    });
    return removeListener;
  }, [addPresenceListener]);

  // Listen for unread updates from other rooms
  useEffect(() => {
    const removeListener = addNotificationListener((notification) => {
      if (notification.type === 'UNREAD_UPDATE' || notification.type === 'NEW_MESSAGE') {
        fetchRooms();
      }
    });
    return removeListener;
  }, [addNotificationListener, fetchRooms]);

  // Load room messages
  useEffect(() => {
    if (!roomId) {
      setCurrentRoom(null);
      setMessages([]);
      setHasMore(false);
      setReplyTo(null);
      setSearchMode(false);
      setSearchKeyword('');
      setSearchResults([]);
      return;
    }

    const loadRoom = async () => {
      setLoading(true);
      setReplyTo(null);
      setSearchMode(false);
      try {
        const [roomRes, msgRes] = await Promise.all([
          chat.getRoom(roomId),
          chat.getMessages(roomId, null, PAGE_SIZE),
        ]);
        setCurrentRoom(roomRes.data.data);
        const msgs = msgRes.data.data || [];
        setMessages(msgs);
        setHasMore(msgs.length >= PAGE_SIZE);
        await chat.markAsRead(roomId);
        fetchRooms();
      } catch (err) {
        console.error('채팅방 로드 실패:', err);
      } finally {
        setLoading(false);
      }
    };
    loadRoom();
  }, [roomId, fetchRooms]);

  // Load more messages
  const loadMoreMessages = useCallback(async () => {
    if (!roomId || loadingMore || !hasMore || messages.length === 0) return;
    setLoadingMore(true);
    try {
      const cursor = messages[0]?.id;
      const res = await chat.getMessages(roomId, cursor, PAGE_SIZE);
      const olderMsgs = res.data.data || [];
      setMessages((prev) => [...olderMsgs, ...prev]);
      setHasMore(olderMsgs.length >= PAGE_SIZE);
    } catch (err) {
      console.error('이전 메시지 로드 실패:', err);
    } finally {
      setLoadingMore(false);
    }
  }, [roomId, loadingMore, hasMore, messages]);

  // WebSocket subscriptions
  useEffect(() => {
    if (!connected || !roomId) return;

    // Message subscription
    const msgDest = `/topic/room/${roomId}`;
    subscribe(msgDest, (msg) => {
      // Handle delete events
      if (msg.type === 'DELETED' && msg.messageId) {
        setMessages((prev) =>
          prev.map((m) =>
            m.id === msg.messageId
              ? { ...m, deleted: true, content: '삭제된 메시지입니다', reactions: [] }
              : m
          )
        );
        return;
      }

      // Handle edit events
      if (msg.type === 'EDITED' && msg.messageId && msg.message) {
        setMessages((prev) =>
          prev.map((m) => m.id === msg.messageId ? msg.message : m)
        );
        return;
      }

      // Handle reaction events
      if (msg.type === 'REACTION' && msg.messageId && msg.message) {
        setMessages((prev) =>
          prev.map((m) => m.id === msg.messageId ? { ...m, reactions: msg.message.reactions } : m)
        );
        return;
      }

      // Normal message
      setMessages((prev) => [...prev, msg]);
      chat.markAsRead(roomId).then(() => fetchRooms()).catch(() => {});
    });

    // Typing subscription
    const typingDest = `/topic/room/${roomId}/typing`;
    subscribe(typingDest, (event) => {
      if (event.userId === user?.id) return;

      setTypingUsers((prev) => {
        const exists = prev.find((u) => u.userId === event.userId);
        if (!exists) return [...prev, { userId: event.userId, userName: event.userName }];
        return prev;
      });

      // Clear existing timeout for this user
      if (typingTimeoutsRef.current.has(event.userId)) {
        clearTimeout(typingTimeoutsRef.current.get(event.userId));
      }

      // Auto-clear after 2 seconds
      const timeout = setTimeout(() => {
        setTypingUsers((prev) => prev.filter((u) => u.userId !== event.userId));
        typingTimeoutsRef.current.delete(event.userId);
      }, 2000);
      typingTimeoutsRef.current.set(event.userId, timeout);
    });

    return () => {
      unsubscribe(msgDest);
      unsubscribe(typingDest);
      typingTimeoutsRef.current.forEach((t) => clearTimeout(t));
      typingTimeoutsRef.current.clear();
      setTypingUsers([]);
    };
  }, [connected, roomId, subscribe, unsubscribe, fetchRooms, user?.id]);

  const handleSendMessage = async (content) => {
    if (!roomId || !content.trim()) return;

    const payload = {
      type: 'TEXT',
      content: content.trim(),
    };

    if (replyTo) {
      payload.replyToMessageId = replyTo.id;
      setReplyTo(null);
    }

    publish(`/app/chat.send/${roomId}`, payload);
  };

  const handleFileUpload = async (file) => {
    if (!roomId) return;

    // Validate file size
    if (file.size > MAX_FILE_SIZE) {
      alert('파일 크기가 10MB를 초과합니다.');
      return;
    }

    // Validate MIME type
    const isAllowed = ALLOWED_MIME_PREFIXES.some((prefix) => file.type.startsWith(prefix));
    if (!isAllowed && file.type) {
      alert('허용되지 않는 파일 형식입니다.');
      return;
    }

    try {
      const res = await chat.uploadFile(roomId, file);
      const fileData = res.data.data;
      publish(`/app/chat.send/${roomId}`, {
        type: 'FILE',
        content: JSON.stringify(fileData),
      });
    } catch (err) {
      console.error('파일 업로드 실패:', err);
    }
  };

  const handleTyping = useCallback(() => {
    if (!roomId || !connected) return;
    publish(`/app/chat.typing/${roomId}`, {});
  }, [roomId, connected, publish]);

  const handleDeleteMessage = useCallback((messageId) => {
    if (!roomId || !connected) return;
    publish(`/app/chat.delete/${roomId}`, { messageId });
  }, [roomId, connected, publish]);

  const handleEditMessage = useCallback((messageId, newContent) => {
    if (!roomId || !connected) return;
    publish(`/app/chat.edit/${roomId}`, { messageId, content: newContent });
  }, [roomId, connected, publish]);

  const handleReaction = useCallback((messageId, emoji) => {
    if (!roomId || !connected) return;
    publish(`/app/chat.react/${roomId}`, { messageId, emoji });
  }, [roomId, connected, publish]);

  const handleReply = useCallback((msg) => {
    setReplyTo(msg);
  }, []);

  // Search
  const handleSearch = useCallback(async (keyword) => {
    if (!roomId || !keyword.trim()) {
      setSearchResults([]);
      return;
    }
    try {
      const res = await chat.searchMessages(roomId, keyword.trim());
      setSearchResults(res.data.data || []);
    } catch (err) {
      console.error('메시지 검색 실패:', err);
    }
  }, [roomId]);

  // D&D handlers
  const handleDragEnter = (e) => {
    e.preventDefault();
    dragCounterRef.current++;
    setIsDragging(true);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    dragCounterRef.current--;
    if (dragCounterRef.current === 0) {
      setIsDragging(false);
    }
  };

  const handleDrop = async (e) => {
    e.preventDefault();
    setIsDragging(false);
    dragCounterRef.current = 0;

    const files = Array.from(e.dataTransfer.files);
    for (const file of files) {
      await handleFileUpload(file);
    }
  };

  const handleRoomSelect = (id) => {
    navigate(`/chat/${id}`);
    if (window.innerWidth < 1024) setSidebarOpen(false);
  };

  const handleCreateRoom = async (data) => {
    try {
      const res = await chat.createRoom(data);
      const newRoom = res.data.data;
      await fetchRooms();
      navigate(`/chat/${newRoom.id}`);
    } catch (err) {
      console.error('채팅방 생성 실패:', err);
      throw err;
    }
  };

  return (
    <div className="flex h-[calc(100vh-8rem)] -m-4 lg:-m-8">
      {/* Sidebar */}
      <div
        className={`${
          sidebarOpen ? 'w-80' : 'w-0'
        } transition-all duration-200 border-r border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 overflow-hidden flex-shrink-0 flex flex-col`}
      >
        <div className="p-4 border-b border-gray-200 dark:border-gray-800 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">채팅</h2>
          <button
            onClick={() => setModalOpen(true)}
            className="p-1.5 text-gray-500 hover:text-primary-500 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
          </button>
        </div>
        <ChatRoomList
          rooms={rooms}
          currentRoomId={roomId ? Number(roomId) : null}
          onSelect={handleRoomSelect}
          onlineUsers={onlineUsers}
        />
      </div>

      {/* Main area */}
      <div
        className="flex-1 flex flex-col bg-gray-50 dark:bg-gray-950 min-w-0 relative"
        onDragEnter={roomId ? handleDragEnter : undefined}
        onDragOver={roomId ? handleDragOver : undefined}
        onDragLeave={roomId ? handleDragLeave : undefined}
        onDrop={roomId ? handleDrop : undefined}
      >
        {/* D&D Overlay */}
        {isDragging && (
          <div className="absolute inset-0 z-50 flex items-center justify-center border-2 border-dashed border-primary-500 bg-primary-500/10 rounded-lg">
            <div className="text-center">
              <svg className="w-12 h-12 text-primary-500 mx-auto mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
              </svg>
              <p className="text-sm font-medium text-primary-600 dark:text-primary-400">파일을 놓아서 업로드</p>
            </div>
          </div>
        )}

        {/* Header */}
        <div className="h-14 px-4 border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 flex items-center gap-3">
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="p-1 text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 rounded"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
            </svg>
          </button>
          {currentRoom ? (
            <div className="flex-1 min-w-0">
              <h3 className="text-sm font-semibold text-gray-900 dark:text-white">
                {currentRoom.name || '1:1 채팅'}
              </h3>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                {currentRoom.participants?.length || 0}명 참여
              </p>
            </div>
          ) : (
            <span className="text-sm text-gray-500 dark:text-gray-400 flex-1">채팅방을 선택하세요</span>
          )}
          {roomId && (
            <button
              onClick={() => { setSearchMode(!searchMode); setSearchResults([]); setSearchKeyword(''); }}
              className={`p-1.5 rounded-lg transition-colors ${searchMode ? 'text-primary-500 bg-primary-50 dark:bg-primary-500/10' : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800'}`}
              title="메시지 검색"
            >
              <svg className="w-4.5 h-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
              </svg>
            </button>
          )}
          {!connected && (
            <span className="text-xs text-amber-500">연결 중...</span>
          )}
        </div>

        {/* Search Panel */}
        {searchMode && (
          <div className="border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 px-4 py-2">
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') handleSearch(searchKeyword); if (e.key === 'Escape') { setSearchMode(false); setSearchResults([]); } }}
                placeholder="메시지 검색..."
                autoFocus
                className="flex-1 px-3 py-1.5 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
              <button
                onClick={() => handleSearch(searchKeyword)}
                className="px-3 py-1.5 text-xs font-medium text-white bg-primary-500 hover:bg-primary-600 rounded-lg"
              >
                검색
              </button>
            </div>
            {searchResults.length > 0 && (
              <div className="mt-2 max-h-48 overflow-y-auto space-y-1">
                {searchResults.map((msg) => (
                  <button
                    key={msg.id}
                    onClick={() => {
                      // Scroll to message if loaded
                      const el = document.getElementById(`msg-${msg.id}`);
                      if (el) {
                        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        el.classList.add('bg-yellow-100', 'dark:bg-yellow-900/30');
                        setTimeout(() => el.classList.remove('bg-yellow-100', 'dark:bg-yellow-900/30'), 2000);
                      }
                    }}
                    className="w-full text-left px-2 py-1.5 rounded hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
                  >
                    <div className="flex items-center gap-2">
                      <span className="text-[11px] font-medium text-gray-600 dark:text-gray-400">{msg.senderName}</span>
                      <span className="text-[10px] text-gray-400">{msg.createdAt ? new Date(msg.createdAt.endsWith('Z') ? msg.createdAt : msg.createdAt + 'Z').toLocaleString('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''}</span>
                    </div>
                    <p className="text-xs text-gray-700 dark:text-gray-300 truncate">{msg.content}</p>
                  </button>
                ))}
              </div>
            )}
            {searchResults.length === 0 && searchKeyword && (
              <p className="mt-2 text-xs text-gray-400">검색 결과가 없습니다</p>
            )}
          </div>
        )}

        {/* Messages */}
        {roomId ? (
          <>
            <ChatMessageArea
              messages={messages}
              currentUserId={user?.id}
              loading={loading}
              hasMore={hasMore}
              onLoadMore={loadMoreMessages}
              loadingMore={loadingMore}
              typingUsers={typingUsers}
              onReply={handleReply}
              onDelete={handleDeleteMessage}
              onEdit={handleEditMessage}
              onReaction={handleReaction}
              onlineUsers={onlineUsers}
            />
            <ChatInput
              onSend={handleSendMessage}
              onFileUpload={handleFileUpload}
              onTyping={handleTyping}
              disabled={!connected}
              replyTo={replyTo}
              onCancelReply={() => setReplyTo(null)}
              participants={currentRoom?.participants || []}
            />
          </>
        ) : (
          <div className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <svg className="w-16 h-16 text-gray-300 dark:text-gray-700 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M8.625 12a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H8.25m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H12m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 01-2.555-.337A5.972 5.972 0 015.41 20.97a5.969 5.969 0 01-.474-.065 4.48 4.48 0 00.978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25z" />
              </svg>
              <p className="text-gray-500 dark:text-gray-400 text-sm">채팅방을 선택하거나 새로 만드세요</p>
            </div>
          </div>
        )}
      </div>

      <ChatCreateModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onSubmit={handleCreateRoom}
      />
    </div>
  );
}
