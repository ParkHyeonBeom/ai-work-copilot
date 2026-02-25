import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { auth } from '../api/endpoints';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const isAuthenticated = !!user;

  /** 현재 사용자 정보를 서버에서 가져오기 */
  const fetchUser = useCallback(async () => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      setIsLoading(false);
      return;
    }

    try {
      const response = await auth.getMe();
      setUser(response.data.data);
    } catch (error) {
      console.error('사용자 정보 조회 실패:', error);
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      setUser(null);
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /** 로그인: 토큰 저장 후 사용자 정보 조회 */
  const login = useCallback(
    async (accessToken, refreshToken) => {
      localStorage.setItem('accessToken', accessToken);
      if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
      }
      await fetchUser();
    },
    [fetchUser]
  );

  /** 로그아웃: 토큰 제거 및 상태 초기화 */
  const logout = useCallback(() => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    setUser(null);
  }, []);

  /** 사용자 정보 갱신 (온보딩 완료 등에서 사용) */
  const refreshUser = useCallback(async () => {
    await fetchUser();
  }, [fetchUser]);

  // 마운트 시 토큰이 있으면 사용자 정보 조회
  useEffect(() => {
    fetchUser().catch(() => {});
  }, [fetchUser]);

  const value = {
    user,
    isAuthenticated,
    isLoading,
    login,
    logout,
    refreshUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.');
  }
  return context;
}

export default AuthContext;
