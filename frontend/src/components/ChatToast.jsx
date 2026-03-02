import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useWebSocketContext } from '../contexts/WebSocketContext';

export default function ChatToast() {
  const [toasts, setToasts] = useState([]);
  const { addNotificationListener } = useWebSocketContext();
  const navigate = useNavigate();
  const location = useLocation();

  const handleNotification = useCallback((notification) => {
    if (notification.type !== 'NEW_MESSAGE') return;

    // Don't show toast if already in that chat room
    const currentPath = location.pathname;
    if (currentPath === `/chat/${notification.roomId}`) return;

    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev.slice(-2), { id, ...notification }]);

    // Auto remove after 5 seconds
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 5000);
  }, [location.pathname]);

  useEffect(() => {
    const removeListener = addNotificationListener(handleNotification);
    return removeListener;
  }, [addNotificationListener, handleNotification]);

  // Browser push notification
  useEffect(() => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  }, []);

  useEffect(() => {
    const handlePush = (notification) => {
      if (notification.type !== 'NEW_MESSAGE') return;
      if (document.visibilityState === 'visible') return;

      if ('Notification' in window && Notification.permission === 'granted') {
        const n = new Notification(`${notification.senderName}`, {
          body: notification.preview || '새 메시지',
          tag: `chat-${notification.roomId}`,
          icon: '/favicon.ico',
        });
        n.onclick = () => {
          window.focus();
          navigate(`/chat/${notification.roomId}`);
          n.close();
        };
      }
    };

    const removeListener = addNotificationListener(handlePush);
    return removeListener;
  }, [addNotificationListener, navigate]);

  if (toasts.length === 0) return null;

  return (
    <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2 max-w-sm">
      {toasts.map((toast) => (
        <button
          key={toast.id}
          onClick={() => {
            navigate(`/chat/${toast.roomId}`);
            setToasts((prev) => prev.filter((t) => t.id !== toast.id));
          }}
          className="flex items-start gap-3 p-3 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-lg hover:shadow-xl transition-all text-left w-full"
        >
          <div className="flex items-center justify-center w-9 h-9 rounded-full bg-primary-100 dark:bg-primary-500/20 shrink-0">
            <svg className="w-4 h-4 text-primary-600 dark:text-primary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M8.625 12a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H8.25m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H12m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 01-2.555-.337A5.972 5.972 0 015.41 20.97a5.969 5.969 0 01-.474-.065 4.48 4.48 0 00.978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25z" />
            </svg>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-xs font-semibold text-gray-900 dark:text-white truncate">
              {toast.roomName || '채팅'}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              <span className="font-medium">{toast.senderName}</span>: {toast.preview || '새 메시지'}
            </p>
          </div>
          <button
            onClick={(e) => {
              e.stopPropagation();
              setToasts((prev) => prev.filter((t) => t.id !== toast.id));
            }}
            className="p-0.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          >
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </button>
      ))}
    </div>
  );
}
