"use client";

import { useCallback } from 'react';
import { AppError, ErrorCode, ERROR_CODES } from '@/lib/error/types';
import { createAppError, logError, shouldRetryError, getRetryDelay } from '@/lib/error/utils';
import { useToast } from '@/hooks/use-toast';

interface UseErrorHandlerOptions {
  showToast?: boolean;
  logToConsole?: boolean;
  reportToService?: boolean;
  fallbackMessage?: string;
  onError?: (error: AppError) => void;
}

interface RetryConfig {
  maxRetries: number;
  retryDelay: number;
  onRetry?: () => Promise<void>;
}

export function useErrorHandler(options: UseErrorHandlerOptions = {}) {
  const {
    showToast = true,
    logToConsole = true,
    reportToService = false,
    onError,
  } = options;

  const { toast } = useToast();

  const handleError = useCallback(
    (
      error: unknown,
      context?: Record<string, unknown>,
      customCode?: ErrorCode,
      customCategory?: string,
      customSeverity?: string,
    ): AppError => {
      const appError = createAppError(
        error,
        customCode || ERROR_CODES.UNKNOWN_ERROR,
        customCategory as
          | 'unknown'
          | 'network'
          | 'authentication'
          | 'authorization'
          | 'validation'
          | 'notFound'
          | 'server'
          | 'client' || 'unknown',
        customSeverity as 'low' | 'medium' | 'high' | 'critical' || 'medium',
        context,
      );

      if (logToConsole) {
        logError(appError, (context?.component as string) || 'useErrorHandler');
      }

      if (showToast) {
        toast({
          title: 'Error',
          description: appError.message,
          variant: 'destructive',
        });
      }

      if (onError) {
        onError(appError);
      }

      if (reportToService) {
        console.log('Error reported to service:', appError);
      }

      return appError;
    },
    [showToast, logToConsole, reportToService, onError, toast],
  );

  const handleAsyncError = useCallback(
    async <T>(
      asyncFn: () => Promise<T>,
      context?: Record<string, unknown>,
      retryConfig?: RetryConfig,
    ): Promise<T | null> => {
      let retryCount = 0;

      const executeWithRetry = async (): Promise<T> => {
        try {
          return await asyncFn();
        } catch (error) {
          const appError = createAppError(
            error,
            ERROR_CODES.UNKNOWN_ERROR,
            'unknown',
            'medium',
            { ...context, retryCount },
          );

          if (retryConfig && shouldRetryError(appError) && retryCount < retryConfig.maxRetries) {
            retryCount++;
            const delay = getRetryDelay(appError.severity, retryCount);

            await new Promise((resolve) => setTimeout(resolve, delay));

            if (retryConfig.onRetry) {
              await retryConfig.onRetry();
            }

            return executeWithRetry();
          }

          handleError(appError, context);
          throw appError;
        }
      };

      try {
        return await executeWithRetry();
      } catch {
        return null;
      }
    },
    [handleError],
  );

  const handleApiError = useCallback(
    (error: unknown, context?: Record<string, unknown>): AppError => {
      let code: ErrorCode = ERROR_CODES.UNKNOWN_ERROR;
      let category = 'unknown';
      let severity = 'medium';

      if (error && typeof error === 'object' && 'status' in error) {
        const status = (error as { status?: number }).status;

        if (status === 401) {
          code = ERROR_CODES.UNAUTHORIZED;
          category = 'authentication';
          severity = 'high';
        } else if (status === 403) {
          code = ERROR_CODES.FORBIDDEN;
          category = 'authorization';
          severity = 'high';
        } else if (status === 404) {
          code = ERROR_CODES.RESOURCE_NOT_FOUND;
          category = 'notFound';
          severity = 'medium';
        } else if (status === 422) {
          code = ERROR_CODES.VALIDATION_ERROR;
          category = 'validation';
          severity = 'low';
        } else if (status && status >= 500) {
          code = ERROR_CODES.INTERNAL_SERVER_ERROR;
          category = 'server';
          severity = 'high';
        }
      }

      return handleError(error, context, code, category, severity);
    },
    [handleError],
  );

  const handleNetworkError = useCallback(
    (error: unknown, context?: Record<string, unknown>): AppError => {
      return handleError(error, context, ERROR_CODES.NETWORK_ERROR, 'network', 'medium');
    },
    [handleError],
  );

  const handleValidationError = useCallback(
    (error: unknown, context?: Record<string, unknown>): AppError => {
      return handleError(error, context, ERROR_CODES.VALIDATION_ERROR, 'validation', 'low');
    },
    [handleError],
  );

  const handleNotFoundError = useCallback(
    (error: unknown, context?: Record<string, unknown>): AppError => {
      return handleError(error, context, ERROR_CODES.RESOURCE_NOT_FOUND, 'notFound', 'medium');
    },
    [handleError],
  );

  const handleAuthError = useCallback(
    (error: unknown, context?: Record<string, unknown>): AppError => {
      return handleError(error, context, ERROR_CODES.UNAUTHORIZED, 'authentication', 'high');
    },
    [handleError],
  );

  const handlePermissionError = useCallback(
    (error: unknown, context?: Record<string, unknown>): AppError => {
      return handleError(error, context, ERROR_CODES.FORBIDDEN, 'authorization', 'high');
    },
    [handleError],
  );

  return {
    handleError,
    handleAsyncError,
    handleApiError,
    handleNetworkError,
    handleValidationError,
    handleNotFoundError,
    handleAuthError,
    handlePermissionError,
  };
}

export default useErrorHandler;
