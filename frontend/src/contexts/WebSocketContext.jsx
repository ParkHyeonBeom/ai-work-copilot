import { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import { useAuth } from './AuthContext';

const WebSocketContext = createContext(null);

export function WebSocketProvider({ children }) {
  const { user } = useAuth();
  const clientRef = useRef(null);
  const [connected, setConnected] = useState(false);
  const subscriptionsRef = useRef(new Map());
  const notificationListenersRef = useRef(new Set());
  const presenceListenersRef = useRef(new Set());

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token || !user) return;

    const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/chat`;

    const stompClient = new Client({
      brokerURL: wsUrl,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: (str) => {
        if (str.includes('ERROR') || str.includes('DISCONNECT')) {
          console.log('[STOMP]', str);
        }
      },
      onConnect: () => {
        console.log('[WS] Connected');
        setConnected(true);

        // Auto-subscribe to personal notifications
        stompClient.subscribe('/user/queue/notifications', (message) => {
          try {
            const body = JSON.parse(message.body);
            notificationListenersRef.current.forEach((listener) => listener(body));
          } catch (e) {
            console.error('[WS] notification parse error:', e);
          }
        });

        // Auto-subscribe to presence updates
        stompClient.subscribe('/topic/presence', (message) => {
          try {
            const body = JSON.parse(message.body);
            presenceListenersRef.current.forEach((listener) => listener(body));
          } catch (e) {
            console.error('[WS] presence parse error:', e);
          }
        });
      },
      onDisconnect: () => {
        console.log('[WS] Disconnected');
        setConnected(false);
      },
      onStompError: (frame) => {
        console.error('[WS] STOMP error:', frame.headers?.message);
        setConnected(false);
      },
      onWebSocketError: (event) => {
        console.error('[WS] WebSocket error:', event);
      },
      onWebSocketClose: () => {
        setConnected(false);
      },
    });

    stompClient.activate();
    clientRef.current = stompClient;

    return () => {
      subscriptionsRef.current.forEach((sub) => sub.unsubscribe());
      subscriptionsRef.current.clear();
      notificationListenersRef.current.clear();
      presenceListenersRef.current.clear();
      if (stompClient.active) {
        stompClient.deactivate();
      }
    };
  }, [user]);

  const subscribe = useCallback((destination, callback) => {
    const client = clientRef.current;
    if (!client || !client.connected) return null;

    if (subscriptionsRef.current.has(destination)) {
      subscriptionsRef.current.get(destination).unsubscribe();
    }

    const subscription = client.subscribe(destination, (message) => {
      try {
        const body = JSON.parse(message.body);
        callback(body);
      } catch {
        callback(message.body);
      }
    });

    subscriptionsRef.current.set(destination, subscription);
    return subscription;
  }, []);

  const unsubscribe = useCallback((destination) => {
    if (subscriptionsRef.current.has(destination)) {
      subscriptionsRef.current.get(destination).unsubscribe();
      subscriptionsRef.current.delete(destination);
    }
  }, []);

  const publish = useCallback((destination, body) => {
    const client = clientRef.current;
    if (!client || !client.connected) return;

    client.publish({
      destination,
      body: typeof body === 'string' ? body : JSON.stringify(body),
    });
  }, []);

  const addNotificationListener = useCallback((listener) => {
    notificationListenersRef.current.add(listener);
    return () => notificationListenersRef.current.delete(listener);
  }, []);

  const addPresenceListener = useCallback((listener) => {
    presenceListenersRef.current.add(listener);
    return () => presenceListenersRef.current.delete(listener);
  }, []);

  const value = { connected, subscribe, unsubscribe, publish, addNotificationListener, addPresenceListener };

  return <WebSocketContext.Provider value={value}>{children}</WebSocketContext.Provider>;
}

export function useWebSocketContext() {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocketContext는 WebSocketProvider 내부에서만 사용할 수 있습니다.');
  }
  return context;
}

export default WebSocketContext;
