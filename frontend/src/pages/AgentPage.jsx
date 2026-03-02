import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { agent } from '../api/endpoints';
import AgentSidebar from '../components/AgentSidebar';
import AgentChatArea from '../components/AgentChatArea';
import AgentInput from '../components/AgentInput';

export default function AgentPage() {
  const { conversationId } = useParams();
  const navigate = useNavigate();

  const [conversations, setConversations] = useState([]);
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [thinking, setThinking] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(true);

  // 대화 목록 조회
  const fetchConversations = useCallback(async () => {
    try {
      const res = await agent.getConversations();
      setConversations(res.data.data || []);
    } catch (err) {
      console.error('대화 목록 조회 실패:', err);
    }
  }, []);

  useEffect(() => {
    fetchConversations();
  }, [fetchConversations]);

  // 선택된 대화의 메시지 로드
  useEffect(() => {
    if (!conversationId) {
      setMessages([]);
      return;
    }

    const loadConversation = async () => {
      setLoading(true);
      try {
        const res = await agent.getConversation(conversationId);
        setMessages(res.data.data?.messages || []);
      } catch (err) {
        console.error('대화 로드 실패:', err);
        if (err?.response?.status === 404) {
          navigate('/agent', { replace: true });
        }
      } finally {
        setLoading(false);
      }
    };
    loadConversation();
  }, [conversationId, navigate]);

  // 메시지 전송
  const handleSend = async (content) => {
    const userMessage = {
      id: `temp-${Date.now()}`,
      role: 'user',
      content,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, userMessage]);
    setThinking(true);

    try {
      const res = await agent.sendMessage({
        conversationId: conversationId || null,
        message: content,
      });

      const data = res.data.data;

      // 새 대화인 경우 URL 업데이트
      if (!conversationId && data.conversationId) {
        navigate(`/agent/${data.conversationId}`, { replace: true });
      }

      const assistantMessage = {
        id: `resp-${Date.now()}`,
        role: 'assistant',
        content: data.reply,
        model: data.model,
        createdAt: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, assistantMessage]);

      await fetchConversations();
    } catch (err) {
      console.error('메시지 전송 실패:', err);
      const errorMessage = {
        id: `err-${Date.now()}`,
        role: 'assistant',
        content: '죄송합니다. 응답을 생성하는 중 오류가 발생했습니다. 다시 시도해 주세요.',
        createdAt: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, errorMessage]);
    } finally {
      setThinking(false);
    }
  };

  // 새 대화
  const handleNewChat = () => {
    navigate('/agent');
    setMessages([]);
  };

  // 대화 선택
  const handleSelect = (id) => {
    navigate(`/agent/${id}`);
    if (window.innerWidth < 1024) setSidebarOpen(false);
  };

  // 대화 삭제
  const handleDelete = async (id) => {
    try {
      await agent.deleteConversation(id);
      await fetchConversations();
      if (String(conversationId) === String(id)) {
        navigate('/agent', { replace: true });
        setMessages([]);
      }
    } catch (err) {
      console.error('대화 삭제 실패:', err);
    }
  };

  return (
    <div className="flex h-[calc(100vh-8rem)] -m-4 lg:-m-8">
      {/* Sidebar */}
      <div
        className={`${
          sidebarOpen ? 'w-72' : 'w-0'
        } transition-all duration-200 border-r border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 overflow-hidden flex-shrink-0`}
      >
        <AgentSidebar
          conversations={conversations}
          currentId={conversationId ? Number(conversationId) : null}
          onSelect={handleSelect}
          onNewChat={handleNewChat}
          onDelete={handleDelete}
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
          <div className="flex items-center gap-2">
            <svg className="w-5 h-5 text-primary-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
            </svg>
            <h3 className="text-sm font-semibold text-gray-900 dark:text-white">AI 어시스턴트</h3>
          </div>
          {thinking && (
            <span className="ml-auto text-xs text-primary-500 animate-pulse">응답 생성 중...</span>
          )}
        </div>

        {/* Chat area */}
        <AgentChatArea
          messages={messages}
          loading={loading}
          thinking={thinking}
        />

        {/* Input */}
        <AgentInput onSend={handleSend} disabled={thinking} />
      </div>
    </div>
  );
}
