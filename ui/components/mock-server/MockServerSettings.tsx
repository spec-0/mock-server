'use client';

import React, { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Save, Trash2, AlertTriangle, Copy, Check, Plus, X } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { MockServer } from '@/services/mockServerAdminClient';
import type { MockServerEnvVar } from '@/services/mockServerAdminClient';
import { useLoading } from '@/hooks/useLoading';
import { SaveButton, DeleteButton } from '@/components/ui/loading-button';

interface MockServerSettingsProps {
  mockServer: MockServer;
  onMockServerUpdate: (updates: { mockServerName: string; description?: string }) => Promise<void>;
  onToggleEnabled?: (enabled: boolean) => Promise<void>;
  onMockServerDelete?: () => Promise<void>;
  // MCP config (standalone only)
  mcpEnabled?: boolean;
  mcpUrl?: string;
  onMcpToggle?: (enabled: boolean) => Promise<void>;
  // Env vars (standalone only — used in CEL expressions as env.KEY)
  envVars?: MockServerEnvVar[];
  onEnvVarCreate?: (key: string, value: string) => Promise<void>;
  onEnvVarDelete?: (envVarId: string) => Promise<void>;
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);
  const handleCopy = async () => {
    await navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };
  return (
    <Button variant="outline" size="sm" onClick={handleCopy} className="shrink-0">
      {copied ? <Check className="h-3 w-3" /> : <Copy className="h-3 w-3" />}
      {copied ? 'Copied' : 'Copy'}
    </Button>
  );
}

export default function MockServerSettings({
  mockServer,
  onMockServerUpdate,
  onToggleEnabled,
  onMockServerDelete,
  mcpEnabled,
  mcpUrl,
  onMcpToggle,
  envVars,
  onEnvVarCreate,
  onEnvVarDelete,
}: MockServerSettingsProps) {
  const { toast } = useToast();
  const { isLoading: isSaving, withLoading: withSaving } = useLoading();
  const { isLoading: isTogglingEnabled, withLoading: withTogglingEnabled } = useLoading();
  const { isLoading: isDeletingMockServer, withLoading: withDeletingMockServer } = useLoading();
  const [mockServerName, setMockServerName] = useState(mockServer.mockServerName);
  const [isEnabled, setIsEnabled] = useState(mockServer.isEnabled);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);

  // Env var form state
  const [newEnvKey, setNewEnvKey] = useState('');
  const [newEnvValue, setNewEnvValue] = useState('');
  const [isAddingEnvVar, setIsAddingEnvVar] = useState(false);

  const handleSave = async () => {
    if (!mockServerName.trim()) {
      toast({ title: 'Invalid Input', description: 'Mock server name cannot be empty.', variant: 'destructive', duration: 5000 });
      return;
    }
    await withSaving(async () => {
      await onMockServerUpdate({ mockServerName: mockServerName.trim() });
      toast({ title: 'Settings Updated Successfully', description: 'Your mock server settings have been saved.', duration: 5000 });
    }, 'Saving Settings...');
  };

  const handleToggleEnabled = async (enabled: boolean) => {
    if (!onToggleEnabled) return;
    await withTogglingEnabled(async () => {
      await onToggleEnabled(enabled);
      setIsEnabled(enabled);
      toast({ title: enabled ? 'Mock Server Enabled' : 'Mock Server Disabled', duration: 5000 });
    }, enabled ? 'Enabling...' : 'Disabling...');
  };

  const handleConfirmDelete = async () => {
    if (!onMockServerDelete) return;
    try {
      await withDeletingMockServer(async () => { await onMockServerDelete(); }, 'Deleting...');
      toast({ title: 'Mock Server Deleted', duration: 5000 });
      setIsDeleteDialogOpen(false);
    } catch (err) {
      toast({ title: 'Failed to Delete', description: err instanceof Error ? err.message : 'Unexpected error.', variant: 'destructive', duration: 7000 });
    }
  };

  const handleAddEnvVar = async () => {
    if (!newEnvKey.trim() || !onEnvVarCreate) return;
    setIsAddingEnvVar(true);
    try {
      await onEnvVarCreate(newEnvKey.trim(), newEnvValue);
      setNewEnvKey('');
      setNewEnvValue('');
      toast({ title: 'Env var added', duration: 3000 });
    } catch (err) {
      toast({ title: 'Failed to add', description: err instanceof Error ? err.message : '', variant: 'destructive' });
    } finally {
      setIsAddingEnvVar(false);
    }
  };

  // Claude Code config snippet
  const claudeSnippet = mcpUrl ? JSON.stringify({
    mcpServers: {
      'spec0-mock': {
        url: mcpUrl,
        description: 'spec0 mock server — create variants, set strategies, inspect logs',
      },
    },
  }, null, 2) : '';

  // Cursor config snippet
  const cursorSnippet = mcpUrl ? JSON.stringify({
    mcpServers: {
      'spec0-mock': {
        serverType: 'sse',
        serverUrl: mcpUrl,
      },
    },
  }, null, 2) : '';

  const showMcp = mcpUrl !== undefined;
  const showEnvVars = envVars !== undefined && onEnvVarCreate !== undefined;

  return (
    <div className="space-y-6">
      {/* Basic Settings */}
      <Card>
        <CardHeader>
          <CardTitle>Basic Settings</CardTitle>
          <CardDescription>Configure basic mock server settings</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="mockServerName">Mock Server Name</Label>
            <Input
              id="mockServerName"
              value={mockServerName}
              onChange={(e) => setMockServerName(e.target.value)}
              placeholder="Enter mock server name"
            />
          </div>
          <div className="flex items-center justify-between">
            <div>
              <Label htmlFor="isEnabled">Enable Mock Server</Label>
              <p className="text-sm text-muted-foreground">When enabled, the mock server will respond to requests</p>
            </div>
            <Switch id="isEnabled" checked={isEnabled} onCheckedChange={handleToggleEnabled} disabled={isTogglingEnabled} />
          </div>
          <div className="flex items-center justify-end space-x-2">
            <Button variant="outline" onClick={() => { setMockServerName(mockServer.mockServerName); setIsEnabled(mockServer.isEnabled); }} disabled={isSaving || isTogglingEnabled}>
              Reset
            </Button>
            <SaveButton onClick={handleSave} isLoading={isSaving} disabled={isTogglingEnabled}>
              Save Settings
            </SaveButton>
          </div>
        </CardContent>
      </Card>

      {/* MCP Server (standalone only) */}
      {showMcp && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="flex items-center gap-2">
                  MCP Server
                  <Badge variant="outline" className={mcpEnabled ? 'text-green-700 border-green-300 bg-green-50' : 'text-gray-500'}>
                    {mcpEnabled ? 'Enabled' : 'Disabled'}
                  </Badge>
                </CardTitle>
                <CardDescription>
                  Expose this mock server as an MCP tool for Claude Code, Cursor, and other AI coding assistants.
                </CardDescription>
              </div>
              {onMcpToggle && (
                <Switch checked={mcpEnabled ?? false} onCheckedChange={onMcpToggle} aria-label="Enable MCP Server" />
              )}
            </div>
          </CardHeader>
          {mcpEnabled && (
            <CardContent className="space-y-4">
              <div>
                <Label className="text-sm font-medium">MCP SSE URL</Label>
                <div className="flex items-center gap-2 mt-1">
                  <code className="flex-1 text-xs bg-muted rounded px-3 py-2 font-mono select-all">{mcpUrl}</code>
                  <CopyButton text={mcpUrl ?? ''} />
                </div>
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label className="text-sm font-medium">Claude Code (.claude/settings.json)</Label>
                  <CopyButton text={claudeSnippet} />
                </div>
                <pre className="text-xs bg-muted rounded p-3 font-mono overflow-x-auto">{claudeSnippet}</pre>
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label className="text-sm font-medium">Cursor (mcp.json)</Label>
                  <CopyButton text={cursorSnippet} />
                </div>
                <pre className="text-xs bg-muted rounded p-3 font-mono overflow-x-auto">{cursorSnippet}</pre>
              </div>

              <div className="rounded-md bg-blue-50 border border-blue-200 p-3 text-sm text-blue-800">
                Once configured, ask your AI assistant: <em>"List my mock servers"</em> or{' '}
                <em>"Create a variant for getUser that returns 404 when the user ID starts with 99"</em>.
              </div>
            </CardContent>
          )}
        </Card>
      )}

      {/* Environment Variables (standalone only — used in CEL expressions) */}
      {showEnvVars && (
        <Card>
          <CardHeader>
            <CardTitle>Environment Variables</CardTitle>
            <CardDescription>
              Key-value pairs accessible in CEL expressions as <code className="font-mono text-xs">env.MY_KEY</code>.
              Useful for per-server config like feature flags, region names, or test data.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {envVars.length > 0 && (
              <div className="rounded-md border divide-y">
                {envVars.map((v) => (
                  <div key={v.envVarId} className="flex items-center justify-between px-3 py-2">
                    <div className="flex items-center gap-3 min-w-0">
                      <code className="text-xs font-mono bg-muted rounded px-1.5 py-0.5 shrink-0">{v.varKey}</code>
                      <span className="text-xs text-muted-foreground truncate">{'•'.repeat(Math.min(v.varValue.length, 12))}</span>
                    </div>
                    {onEnvVarDelete && (
                      <Button variant="ghost" size="sm" onClick={() => onEnvVarDelete(v.envVarId)} className="shrink-0 text-destructive hover:text-destructive">
                        <X className="h-3 w-3" />
                      </Button>
                    )}
                  </div>
                ))}
              </div>
            )}

            <div className="flex items-end gap-2">
              <div className="flex-1">
                <Label htmlFor="envKey" className="text-xs">Key</Label>
                <Input
                  id="envKey"
                  placeholder="MY_KEY"
                  value={newEnvKey}
                  onChange={(e) => setNewEnvKey(e.target.value)}
                  className="font-mono text-sm"
                />
              </div>
              <div className="flex-1">
                <Label htmlFor="envValue" className="text-xs">Value</Label>
                <Input
                  id="envValue"
                  placeholder="my-value"
                  value={newEnvValue}
                  onChange={(e) => setNewEnvValue(e.target.value)}
                />
              </div>
              <Button size="sm" onClick={handleAddEnvVar} disabled={!newEnvKey.trim() || isAddingEnvVar}>
                <Plus className="h-4 w-4 mr-1" />
                Add
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* API Information */}
      <Card>
        <CardHeader>
          <CardTitle>API Information</CardTitle>
          <CardDescription>Information about your mock server API</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <Label className="text-sm font-medium">Mock Server ID</Label>
              <p className="text-sm text-muted-foreground font-mono">{mockServer.mockServerId}</p>
            </div>
            <div>
              <Label className="text-sm font-medium">API ID</Label>
              <p className="text-sm text-muted-foreground font-mono">{mockServer.apiId}</p>
            </div>
            <div>
              <Label className="text-sm font-medium">Created By</Label>
              <p className="text-sm text-muted-foreground">{mockServer.createdBy ?? '—'}</p>
            </div>
            <div>
              <Label className="text-sm font-medium">Created At</Label>
              <p className="text-sm text-muted-foreground">{new Date(mockServer.createdAt).toLocaleString()}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Danger Zone */}
      <Card className="border-red-200">
        <CardHeader>
          <CardTitle className="text-red-600">Danger Zone</CardTitle>
          <CardDescription>Irreversible and destructive actions</CardDescription>
        </CardHeader>
        <CardContent>
          <Alert variant="destructive">
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>
              <strong>Warning:</strong> Deleting this mock server will permanently remove all associated data including variants, logs, and env vars. This action cannot be undone.
            </AlertDescription>
          </Alert>
          <div className="mt-4">
            <Button variant="destructive" onClick={() => setIsDeleteDialogOpen(true)} disabled={isSaving || isTogglingEnabled || isDeletingMockServer}>
              <Trash2 className="h-4 w-4 mr-2" />
              Delete Mock Server
            </Button>
          </div>
        </CardContent>
      </Card>

      <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Delete Mock Server?</DialogTitle>
            <DialogDescription>
              This action cannot be undone. All variants, logs, and env vars will be permanently deleted.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setIsDeleteDialogOpen(false)} disabled={isDeletingMockServer}>
              Cancel
            </Button>
            <DeleteButton variant="destructive" type="button" onClick={handleConfirmDelete} isLoading={isDeletingMockServer} disabled={isDeletingMockServer}>
              Delete
            </DeleteButton>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
