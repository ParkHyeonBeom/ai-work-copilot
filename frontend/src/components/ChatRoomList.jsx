const typeLabels = { DIRECT: '1:1', GROUP: '그룹', GENERAL: '전체' };

export default function ChatRoomList({ rooms, currentRoomId, onSelect }) {
  if (!rooms.length) {
    return (
      <div className="flex-1 flex items-center justify-center p-4">
        <p className="text-sm text-gray-400 dark:text-gray-600 text-center">
          아직 채팅방이 없습니다.<br />새 채팅을 시작해보세요.
        </p>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto">
      {rooms.map((room) => (
        <button
          key={room.id}
          onClick={() => onSelect(room.id)}
          className={`w-full text-left px-4 py-3 border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors ${
            currentRoomId === room.id ? 'bg-primary-50 dark:bg-primary-500/10' : ''
          }`}
        >
          <div className="flex items-center justify-between mb-1">
            <div className="flex items-center gap-2 min-w-0">
              <span className="text-[10px] font-medium px-1.5 py-0.5 rounded bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400 shrink-0">
                {typeLabels[room.type] || room.type}
              </span>
              <span className="text-sm font-medium text-gray-900 dark:text-white truncate">
                {room.name || room.participants?.map((p) => p.userName).join(', ') || '채팅방'}
              </span>
            </div>
            {room.unreadCount > 0 && (
              <span className="ml-2 inline-flex items-center justify-center min-w-[20px] h-5 px-1.5 text-[10px] font-bold text-white bg-red-500 rounded-full shrink-0">
                {room.unreadCount > 99 ? '99+' : room.unreadCount}
              </span>
            )}
          </div>
          {room.lastMessageContent && (
            <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
              {room.lastMessageContent}
            </p>
          )}
          {room.lastMessageAt && (
            <p className="text-[10px] text-gray-400 dark:text-gray-600 mt-0.5">
              {new Date(room.lastMessageAt).toLocaleString('ko-KR', {
                month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
              })}
            </p>
          )}
        </button>
      ))}
    </div>
  );
}
