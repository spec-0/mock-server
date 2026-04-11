import React from 'react';
import { Button, ButtonProps } from '@/components/ui/button';
import { Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';

interface LoadingButtonProps extends Omit<ButtonProps, 'disabled'> {
  isLoading?: boolean;
  loadingText?: string;
  loadingIcon?: React.ReactNode;
  disabled?: boolean;
}

/**
 * Generic loading button component that provides consistent loading behavior
 * across the entire application for save, update, create, and other actions
 */
export const LoadingButton = React.forwardRef<HTMLButtonElement, LoadingButtonProps>(
  ({ 
    isLoading = false, 
    loadingText, 
    loadingIcon, 
    disabled = false,
    children,
    className,
    ...props 
  }, ref) => {
    const isDisabled = disabled || isLoading;
    const displayText = isLoading ? (loadingText || 'Loading...') : children;
    const displayIcon = isLoading ? (
      loadingIcon || <Loader2 className="h-4 w-4 animate-spin" />
    ) : null;

    return (
      <Button
        ref={ref}
        disabled={isDisabled}
        className={cn(
          "relative",
          isLoading && "cursor-not-allowed",
          className
        )}
        {...props}
      >
        {displayIcon && (
          <span className="mr-2">
            {displayIcon}
          </span>
        )}
        {displayText}
      </Button>
    );
  }
);

LoadingButton.displayName = 'LoadingButton';

/**
 * Specialized loading button variants for common actions
 */
export const SaveButton = React.forwardRef<HTMLButtonElement, Omit<LoadingButtonProps, 'loadingText'>>(
  ({ isLoading, children = 'Save', ...props }, ref) => (
    <LoadingButton
      ref={ref}
      isLoading={isLoading}
      loadingText="Saving..."
      {...props}
    >
      {children}
    </LoadingButton>
  )
);

SaveButton.displayName = 'SaveButton';

export const UpdateButton = React.forwardRef<HTMLButtonElement, Omit<LoadingButtonProps, 'loadingText'>>(
  ({ isLoading, children = 'Update', ...props }, ref) => (
    <LoadingButton
      ref={ref}
      isLoading={isLoading}
      loadingText="Updating..."
      {...props}
    >
      {children}
    </LoadingButton>
  )
);

UpdateButton.displayName = 'UpdateButton';

export const CreateButton = React.forwardRef<HTMLButtonElement, Omit<LoadingButtonProps, 'loadingText'>>(
  ({ isLoading, children = 'Create', ...props }, ref) => (
    <LoadingButton
      ref={ref}
      isLoading={isLoading}
      loadingText="Creating..."
      {...props}
    >
      {children}
    </LoadingButton>
  )
);

CreateButton.displayName = 'CreateButton';

export const DeleteButton = React.forwardRef<HTMLButtonElement, Omit<LoadingButtonProps, 'loadingText'>>(
  ({ isLoading, children = 'Delete', variant = 'destructive', ...props }, ref) => (
    <LoadingButton
      ref={ref}
      isLoading={isLoading}
      loadingText="Deleting..."
      variant={variant}
      {...props}
    >
      {children}
    </LoadingButton>
  )
);

DeleteButton.displayName = 'DeleteButton';

export const TestButton = React.forwardRef<HTMLButtonElement, Omit<LoadingButtonProps, 'loadingText'>>(
  ({ isLoading, children = 'Test', ...props }, ref) => (
    <LoadingButton
      ref={ref}
      isLoading={isLoading}
      loadingText="Testing..."
      {...props}
    >
      {children}
    </LoadingButton>
  )
);

TestButton.displayName = 'TestButton';
