/**
 * Error handling utilities for consistent error management
 */

import { AppError, ErrorSeverity, ErrorCategory, ErrorCode, ERROR_CODES } from './types';

/**
 * Creates a standardized AppError from various error sources
 */
export function createAppError(
  error: unknown,
  code: ErrorCode = ERROR_CODES.UNKNOWN_ERROR,
  category: ErrorCategory = 'unknown',
  severity: ErrorSeverity = 'medium',
  context?: Record<string, unknown>
): AppError {
  let message = 'An unexpected error occurred';
  let details: string | undefined;
  let stack: string | undefined;

  if (error instanceof Error) {
    message = error.message;
    stack = error.stack;
  } else if (typeof error === 'string') {
    message = error;
  } else if (error && typeof error === 'object' && 'message' in error) {
    message = String(error.message);
  }

  // Extract details from fetch errors
  if (error && typeof error === 'object' && 'status' in error) {
    const status = (error as any).status;
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
    } else if (status >= 500) {
      code = ERROR_CODES.INTERNAL_SERVER_ERROR;
      category = 'server';
      severity = 'high';
    }
  }

  return {
    code,
    message,
    details,
    timestamp: new Date(),
    context,
    stack,
    severity,
    category,
  };
}

/**
 * Determines error severity based on error type and context
 */
export function determineErrorSeverity(
  error: unknown,
  context?: Record<string, unknown>
): ErrorSeverity {
  if (error instanceof Error) {
    // Network errors are usually medium severity
    if (error.message.includes('fetch') || error.message.includes('network')) {
      return 'medium';
    }
    
    // Authentication errors are high severity
    if (error.message.includes('unauthorized') || error.message.includes('token')) {
      return 'high';
    }
    
    // Validation errors are usually low severity
    if (error.message.includes('validation') || error.message.includes('invalid')) {
      return 'low';
    }
  }

  // Check context for severity indicators
  if (context?.critical) {
    return 'critical';
  }

  return 'medium';
}

/**
 * Determines error category based on error type and context
 */
export function determineErrorCategory(
  error: unknown,
  context?: Record<string, unknown>
): ErrorCategory {
  if (error instanceof Error) {
    const message = error.message.toLowerCase();
    
    if (message.includes('network') || message.includes('fetch') || message.includes('timeout')) {
      return 'network';
    }
    
    if (message.includes('unauthorized') || message.includes('token') || message.includes('auth')) {
      return 'authentication';
    }
    
    if (message.includes('forbidden') || message.includes('permission')) {
      return 'authorization';
    }
    
    if (message.includes('validation') || message.includes('invalid')) {
      return 'validation';
    }
    
    if (message.includes('not found') || message.includes('404')) {
      return 'notFound';
    }
  }

  // Check context for category indicators
  if (context?.category) {
    return context.category as ErrorCategory;
  }

  return 'unknown';
}

/**
 * Generates a user-friendly error message based on error category
 */
export function getUserFriendlyMessage(error: AppError): string {
  const { category, code, message } = error;

  switch (category) {
    case 'network':
      return 'Unable to connect to the server. Please check your internet connection and try again.';
    
    case 'authentication':
      if (code === ERROR_CODES.TOKEN_EXPIRED) {
        return 'Your session has expired. Please sign in again.';
      }
      return 'Authentication failed. Please sign in again.';
    
    case 'authorization':
      return 'You do not have permission to perform this action.';
    
    case 'validation':
      return `Please check your input: ${message}`;
    
    case 'notFound':
      return 'The requested resource was not found.';
    
    case 'server':
      return 'Something went wrong on our end. Please try again later.';
    
    case 'client':
      return 'Something went wrong. Please refresh the page and try again.';
    
    default:
      return 'An unexpected error occurred. Please try again.';
  }
}

/**
 * Determines if an error should be retried
 */
export function shouldRetryError(error: AppError): boolean {
  const { category, code } = error;

  // Don't retry authentication or authorization errors
  if (category === 'authentication' || category === 'authorization') {
    return false;
  }

  // Don't retry validation errors
  if (category === 'validation') {
    return false;
  }

  // Retry network and server errors
  if (category === 'network' || category === 'server') {
    return true;
  }

  // Don't retry client errors
  if (category === 'client') {
    return false;
  }

  return false;
}

/**
 * Gets retry delay based on error severity and retry count
 */
export function getRetryDelay(severity: ErrorSeverity, retryCount: number): number {
  const baseDelay = severity === 'critical' ? 1000 : 2000;
  const exponentialDelay = baseDelay * Math.pow(2, retryCount);
  const maxDelay = 30000; // 30 seconds max
  
  return Math.min(exponentialDelay, maxDelay);
}

/**
 * Logs error to console with appropriate level
 */
export function logError(error: AppError, context?: string): void {
  const logMessage = `[${error.category.toUpperCase()}] ${error.message}`;
  const logContext = context ? ` (${context})` : '';
  
  switch (error.severity) {
    case 'critical':
      console.error(`🚨 CRITICAL${logContext}:`, logMessage, error);
      break;
    case 'high':
      console.error(`❌ HIGH${logContext}:`, logMessage, error);
      break;
    case 'medium':
      console.warn(`⚠️ MEDIUM${logContext}:`, logMessage, error);
      break;
    case 'low':
      console.info(`ℹ️ LOW${logContext}:`, logMessage, error);
      break;
  }
}

/**
 * Generates a unique error ID for tracking
 */
export function generateErrorId(): string {
  return `err_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
}
