import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './contexts/AuthContext';
import { WebSocketProvider } from './contexts/WebSocketContext';
import Layout from './components/Layout';
import ChatToast from './components/ChatToast';
import LoginPage from './pages/LoginPage';
import OAuthCallbackPage from './pages/OAuthCallbackPage';
import OnboardingPage from './pages/OnboardingPage';
import DashboardPage from './pages/DashboardPage';
import BriefingPage from './pages/BriefingPage';
import PendingApprovalPage from './pages/PendingApprovalPage';
import VerifyEmailPage from './pages/VerifyEmailPage';
import RejectedPage from './pages/RejectedPage';
import AdminPage from './pages/AdminPage';
import CalendarPage from './pages/CalendarPage';
import ChatPage from './pages/ChatPage';
import AgentPage from './pages/AgentPage';

/**
 * 인증이 필요한 라우트를 보호하는 컴포넌트
 */
function ProtectedRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-950">
        <div className="text-center">
          <div className="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
          <p className="text-sm text-gray-500 dark:text-gray-400">
            로딩 중...
          </p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

/**
 * 온보딩이 완료되지 않으면 온보딩으로 리다이렉트
 */
function OnboardedRoute({ children }) {
  const { user } = useAuth();

  if (user && !user.onboardingCompleted) {
    return <Navigate to="/onboarding" replace />;
  }

  return children;
}

export default function App() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
      <Route path="/pending-approval" element={<PendingApprovalPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/rejected" element={<RejectedPage />} />

      {/* Onboarding (protected but no layout) */}
      <Route
        path="/onboarding"
        element={
          <ProtectedRoute>
            <OnboardingPage />
          </ProtectedRoute>
        }
      />

      {/* Protected routes with layout */}
      <Route
        element={
          <ProtectedRoute>
            <OnboardedRoute>
              <WebSocketProvider>
                <ChatToast />
                <Layout />
              </WebSocketProvider>
            </OnboardedRoute>
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/briefing/:id" element={<BriefingPage />} />
        <Route path="/briefings" element={<BriefingPage />} />
        <Route path="/calendar" element={<CalendarPage />} />
        <Route path="/chat" element={<ChatPage />} />
        <Route path="/chat/:roomId" element={<ChatPage />} />
        <Route path="/agent" element={<AgentPage />} />
        <Route path="/agent/:conversationId" element={<AgentPage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Route>

      {/* Default redirect */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
