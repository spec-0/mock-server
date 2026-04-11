"use client";

import React, { Component, ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { AlertCircle, ChevronDown, ChevronUp, Home, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { AppError, ERROR_CODES } from '@/lib/error/types';
import { createAppError, getUserFriendlyMessage, logError, generateErrorId } from '@/lib/error/utils';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: AppError, errorInfo: React.ErrorInfo) => void;
  resetOnPropsChange?: boolean;
  resetKeys?: Array<string | number>;
  level?: 'page' | 'section' | 'component';
  showDetails?: boolean;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error?: AppError;
  errorId?: string;
  showDetails: boolean;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  private resetTimeoutId: ReturnType<typeof setTimeout> | null = null;

  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = {
      hasError: false,
      showDetails: false,
    };
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    const appError = createAppError(error, ERROR_CODES.COMPONENT_ERROR, 'client', 'medium');
    const errorId = generateErrorId();

    return {
      hasError: true,
      error: appError,
      errorId,
    };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    const appError = createAppError(error, ERROR_CODES.COMPONENT_ERROR, 'client', 'medium', {
      componentStack: errorInfo.componentStack,
      errorBoundary: this.props.level || 'component',
    });

    const errorId = generateErrorId();

    this.setState({ error: appError, errorId });

    logError(appError, `ErrorBoundary (${this.props.level || 'component'})`);

    if (this.props.onError) {
      this.props.onError(appError, errorInfo);
    }
  }

  componentDidUpdate(prevProps: ErrorBoundaryProps) {
    const { resetKeys, resetOnPropsChange } = this.props;
    const { hasError } = this.state;

    if (hasError && resetOnPropsChange && resetKeys) {
      const hasResetKeyChanged = resetKeys.some((key, index) => key !== prevProps.resetKeys?.[index]);

      if (hasResetKeyChanged) {
        this.resetErrorBoundary();
      }
    }
  }

  componentWillUnmount() {
    if (this.resetTimeoutId) {
      clearTimeout(this.resetTimeoutId);
    }
  }

  private resetErrorBoundary = () => {
    if (this.resetTimeoutId) {
      clearTimeout(this.resetTimeoutId);
    }

    this.setState({
      hasError: false,
      error: undefined,
      errorId: undefined,
      showDetails: false,
    });
  };

  private handleRetry = () => {
    this.resetErrorBoundary();
  };

  private toggleDetails = () => {
    this.setState((prevState) => ({
      showDetails: !prevState.showDetails,
    }));
  };

  render() {
    const { hasError, error, errorId, showDetails } = this.state;
    const { children, fallback, showDetails: propShowDetails = false } = this.props;

    if (hasError) {
      if (fallback) {
        return fallback;
      }

      const userMessage = error ? getUserFriendlyMessage(error) : 'Something went wrong';
      const canRetry = error?.category !== 'authentication' && error?.category !== 'authorization';

      return (
        <ErrorBoundaryContent
          error={error}
          errorId={errorId}
          userMessage={userMessage}
          canRetry={canRetry}
          showDetails={showDetails || propShowDetails}
          onRetry={this.handleRetry}
          onToggleDetails={this.toggleDetails}
          level={this.props.level || 'component'}
        />
      );
    }

    return children;
  }
}

interface ErrorBoundaryContentProps {
  error?: AppError;
  errorId?: string;
  userMessage: string;
  canRetry: boolean;
  showDetails: boolean;
  onRetry: () => void;
  onToggleDetails: () => void;
  level: 'page' | 'section' | 'component';
}

function ErrorBoundaryContent({
  error,
  errorId,
  userMessage,
  canRetry,
  showDetails,
  onRetry,
  onToggleDetails,
  level,
}: ErrorBoundaryContentProps) {
  const router = useRouter();

  const handleGoHome = () => {
    router.push('/');
  };

  const iconSize = level === 'page' ? 'h-12 w-12' : level === 'section' ? 'h-9 w-9' : 'h-6 w-6';
  const titleClass = level === 'page' ? 'text-2xl font-semibold' : 'text-lg font-semibold';

  return (
    <div className={level === 'page' ? 'p-8' : 'p-4'}>
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col items-center text-center gap-4">
            <AlertCircle className={`${iconSize} text-destructive`} />

            <div>
              <h2 className={`${titleClass} mb-2`}>Something went wrong</h2>
              <p className="text-muted-foreground text-sm mb-2">{userMessage}</p>
              {errorId && <p className="text-xs text-muted-foreground">Error ID: {errorId}</p>}
            </div>

            {error && (
              <div className="flex flex-wrap gap-2 justify-center">
                <Badge variant="destructive">{error.category}</Badge>
                <Badge variant="outline">{error.severity}</Badge>
                <Badge variant="outline">{error.code}</Badge>
              </div>
            )}

            <div className="flex flex-wrap gap-2 justify-center">
              {canRetry && (
                <Button onClick={onRetry}>
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Try again
                </Button>
              )}
              <Button variant="outline" onClick={handleGoHome}>
                <Home className="h-4 w-4 mr-2" />
                Go home
              </Button>
            </div>

            {error && (
              <div className="w-full max-w-lg">
                <Button variant="ghost" size="sm" className="gap-1" onClick={onToggleDetails}>
                  {showDetails ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                  {showDetails ? 'Hide' : 'Show'} technical details
                </Button>
                {showDetails && (
                  <Alert className="mt-2 text-left">
                    <AlertTitle>Technical details</AlertTitle>
                    <AlertDescription>
                      <pre className="text-xs font-mono whitespace-pre-wrap break-words max-h-48 overflow-auto mt-2">
                        {error.stack || error.message}
                      </pre>
                    </AlertDescription>
                  </Alert>
                )}
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export function useErrorHandler() {
  const throwError = (error: unknown, context?: Record<string, unknown>) => {
    const appError = createAppError(error, ERROR_CODES.COMPONENT_ERROR, 'client', 'medium', context);
    logError(appError, 'useErrorHandler');
    throw appError;
  };

  return { throwError };
}

export default ErrorBoundary;
