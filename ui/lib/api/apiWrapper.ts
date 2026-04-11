/**
 * API wrapper with consistent error handling
 */

import { AppError, ERROR_CODES, ErrorCode, ErrorCategory, ErrorSeverity } from '@/lib/error/types';
import { createAppError, logError } from '@/lib/error/utils';

export interface ApiResponse<T> {
  data?: T;
  error?: AppError;
  success: boolean;
}

/**
 * Wraps API calls with consistent error handling
 */
export async function withErrorHandling<T>(
  apiCall: () => Promise<T>,
  context?: Record<string, unknown>
): Promise<ApiResponse<T>> {
  try {
    const data = await apiCall();
    return {
      data,
      success: true,
    };
  } catch (error) {
    const appError = createAppError(
      error,
      ERROR_CODES.NETWORK_ERROR,
      'network',
      'medium',
      context
    );

    logError(appError, 'API Wrapper');

    return {
      error: appError,
      success: false,
    };
  }
}

/**
 * Wraps fetch calls with consistent error handling
 */
export async function fetchWithErrorHandling<T>(
  url: string,
  options: RequestInit,
  context?: Record<string, unknown>
): Promise<ApiResponse<T>> {
  try {
    const response = await fetch(url, options);
    
    if (!response.ok) {
      let errorCode: ErrorCode = ERROR_CODES.NETWORK_ERROR;
      let category: ErrorCategory = 'network';
      let severity: ErrorSeverity = 'medium';

      if (response.status === 401) {
        errorCode = ERROR_CODES.UNAUTHORIZED;
        category = 'authentication';
        severity = 'high';
      } else if (response.status === 403) {
        errorCode = ERROR_CODES.FORBIDDEN;
        category = 'authorization';
        severity = 'high';
      } else if (response.status === 404) {
        errorCode = ERROR_CODES.RESOURCE_NOT_FOUND;
        category = 'notFound';
        severity = 'medium';
      } else if (response.status === 422) {
        errorCode = ERROR_CODES.VALIDATION_ERROR;
        category = 'validation';
        severity = 'low';
      } else if (response.status >= 500) {
        errorCode = ERROR_CODES.INTERNAL_SERVER_ERROR;
        category = 'server';
        severity = 'high';
      }

      const errorText = await response.text();
      let errorMessage = `HTTP ${response.status}: ${response.statusText}`;
      
      try {
        const errorJson = JSON.parse(errorText);
        errorMessage = errorJson.message || errorJson.detail || errorMessage;
      } catch {
        // Use the text as is if it's not JSON
        if (errorText) {
          errorMessage = errorText;
        }
      }

      const appError = createAppError(
        new Error(errorMessage),
        errorCode,
        category,
        severity,
        {
          ...context,
          status: response.status,
          statusText: response.statusText,
          url,
        }
      );

      logError(appError, 'Fetch Wrapper');

      return {
        error: appError,
        success: false,
      };
    }

    const data = await response.json();
    return {
      data,
      success: true,
    };
  } catch (error) {
    const appError = createAppError(
      error,
      ERROR_CODES.NETWORK_ERROR,
      'network',
      'medium',
      { ...context, url }
    );

    logError(appError, 'Fetch Wrapper');

    return {
      error: appError,
      success: false,
    };
  }
}

/**
 * Retry wrapper for API calls
 */
export async function withRetry<T>(
  apiCall: () => Promise<T>,
  maxRetries: number = 3,
  baseDelay: number = 1000,
  context?: Record<string, unknown>
): Promise<ApiResponse<T>> {
  let lastError: AppError | null = null;

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      const data = await apiCall();
      return {
        data,
        success: true,
      };
    } catch (error) {
      lastError = createAppError(
        error,
        ERROR_CODES.NETWORK_ERROR,
        'network',
        'medium',
        { ...context, attempt, maxRetries }
      );

      if (attempt === maxRetries) {
        logError(lastError, 'Retry Wrapper');
        return {
          error: lastError,
          success: false,
        };
      }

      // Exponential backoff
      const delay = baseDelay * Math.pow(2, attempt);
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }

  return {
    error: lastError!,
    success: false,
  };
}
