import { useState, useCallback } from 'react';

export interface LoadingState {
  isLoading: boolean;
  loadingMessage?: string;
}

export interface UseLoadingReturn {
  isLoading: boolean;
  loadingMessage?: string;
  setLoading: (loading: boolean, message?: string) => void;
  withLoading: <T>(
    asyncFn: () => Promise<T>,
    loadingMessage?: string
  ) => Promise<T>;
}

/**
 * Generic loading hook for managing loading states across the application
 * Provides consistent loading behavior for all manual actions
 */
export function useLoading(initialState: LoadingState = { isLoading: false }): UseLoadingReturn {
  const [isLoading, setIsLoading] = useState(initialState.isLoading);
  const [loadingMessage, setLoadingMessage] = useState(initialState.loadingMessage);

  const setLoading = useCallback((loading: boolean, message?: string) => {
    setIsLoading(loading);
    setLoadingMessage(message);
  }, []);

  const withLoading = useCallback(async <T>(
    asyncFn: () => Promise<T>,
    message?: string
  ): Promise<T> => {
    setLoading(true, message);
    try {
      const result = await asyncFn();
      return result;
    } finally {
      setLoading(false);
    }
  }, [setLoading]);

  return {
    isLoading,
    loadingMessage,
    setLoading,
    withLoading,
  };
}

/**
 * Hook for managing multiple loading states simultaneously
 * Useful for components that have multiple async operations
 */
export function useMultipleLoading() {
  const [loadingStates, setLoadingStates] = useState<Record<string, LoadingState>>({});

  const setLoading = useCallback((key: string, loading: boolean, message?: string) => {
    setLoadingStates(prev => ({
      ...prev,
      [key]: { isLoading: loading, loadingMessage: message }
    }));
  }, []);

  const isLoading = useCallback((key: string) => {
    return loadingStates[key]?.isLoading || false;
  }, [loadingStates]);

  const getLoadingMessage = useCallback((key: string) => {
    return loadingStates[key]?.loadingMessage;
  }, [loadingStates]);

  const withLoading = useCallback(async <T>(
    key: string,
    asyncFn: () => Promise<T>,
    message?: string
  ): Promise<T> => {
    setLoading(key, true, message);
    try {
      const result = await asyncFn();
      return result;
    } finally {
      setLoading(key, false);
    }
  }, [setLoading]);

  const isAnyLoading = Object.values(loadingStates).some(state => state.isLoading);

  return {
    setLoading,
    isLoading,
    getLoadingMessage,
    withLoading,
    isAnyLoading,
    loadingStates,
  };
}
