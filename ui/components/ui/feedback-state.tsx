'use client';

import React from 'react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { AlertCircle, Inbox, RefreshCw } from 'lucide-react';

interface FeedbackStateProps {
  type: 'error' | 'empty' | 'info';
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
}

export function FeedbackState({
  type,
  title,
  description,
  actionLabel,
  onAction,
}: FeedbackStateProps) {
  const isError = type === 'error';
  const Icon = isError ? AlertCircle : Inbox;

  return (
    <Alert variant={isError ? 'destructive' : 'default'} className="py-4">
      <Icon className="h-4 w-4" />
      <AlertTitle>{title}</AlertTitle>
      <AlertDescription className="space-y-3">
        <p>{description}</p>
        {actionLabel && onAction && (
          <Button variant={isError ? 'outline' : 'secondary'} size="sm" onClick={onAction}>
            <RefreshCw className="h-4 w-4 mr-2" />
            {actionLabel}
          </Button>
        )}
      </AlertDescription>
    </Alert>
  );
}
