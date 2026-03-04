import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { chat } from '../api/endpoints';
import { useAuth } from '../contexts/AuthContext';
import useWebSocket from '../hooks/useWebSocket';
import ChatRoomList from '../components/ChatRoomList';
import ChatMessageArea from '../components/ChatMessageArea';
import ChatInput from '../components/ChatInput';
import ChatCreateModal from '../components/ChatCreateModal';

export default function ChatPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { connected, subscribe, unsubscribe, publish, addNotificationListener } = useWebSocket();

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
  const typingTimeoutsRef = useRef(new Map());
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
      return;
    }

    const loadRoom = async () => {
      setLoading(true);
      setReplyTo(null);
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
              ? { ...m, deleted: true, content: '삭제된 메시지입니다' }
              : m
          )
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

  const handleReply = useCallback((msg) => {
    setReplyTo(msg);
  }, []);

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
        />
      </div>

      {/* Main area */}
      <div className="flex-1 flex flex-col bg-gray-50 dark:bg-gray-950 min-w-0">
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
            <div>
              <h3 className="text-sm font-semibold text-gray-900 dark:text-white">
                {currentRoom.name || '1:1 채팅'}
              </h3>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                {currentRoom.participants?.length || 0}명 참여
              </p>
            </div>
          ) : (
            <span className="text-sm text-gray-500 dark:text-gray-400">채팅방을 선택하세요</span>
          )}
          {!connected && (
            <span className="ml-auto text-xs text-amber-500">연결 중...</span>
          )}
        </div>

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
            />
            <ChatInput
              onSend={handleSendMessage}
              onFileUpload={handleFileUpload}
              onTyping={handleTyping}
              disabled={!connected}
              replyTo={replyTo}
              onCancelReply={() => setReplyTo(null)}
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
