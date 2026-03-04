import { useWebSocketContext } from '../contexts/WebSocketContext';

export default function useWebSocket() {
  return useWebSocketContext();
}
