'use client';

import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Save, RefreshCw, RotateCcw } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { MockServerConfig } from '@/services/mockServerAdminClient';

interface MockServerConfigurationProps {
  config: MockServerConfig | null;
  onConfigUpdate: (config: MockServerConfig) => Promise<void>;
  onSyncOperations?: () => Promise<void>;
}

export default function MockServerConfiguration({ config, onConfigUpdate, onSyncOperations }: MockServerConfigurationProps) {
  const { toast } = useToast();
  const [isLoading, setIsLoading] = useState(false);
  const [isSyncing, setIsSyncing] = useState(false);
  const [formData, setFormData] = useState<MockServerConfig>({
    defaultStrategy: config?.defaultStrategy || 'RANDOM',
    validationMode: config?.validationMode || 'OFF',
    maxLogEntries: config?.maxLogEntries || 100,
    rateLimitPerMinute: config?.rateLimitPerMinute || 60,
    maxVariantsPerOperation: config?.maxVariantsPerOperation || 10,
    maxTotalVariants: config?.maxTotalVariants || 100,
  });

  // Config loads async; initial useState ran while config was null, so validationMode/strategy
  // stayed at defaults. Without this sync, Save would PATCH stale values (e.g. OFF) and wipe DB.
  useEffect(() => {
    if (!config) return;
    setFormData({
      defaultStrategy: config.defaultStrategy || 'RANDOM',
      validationMode: config.validationMode || 'OFF',
      maxLogEntries: config.maxLogEntries ?? 100,
      rateLimitPerMinute: config.rateLimitPerMinute ?? 60,
      maxVariantsPerOperation: config.maxVariantsPerOperation ?? 10,
      maxTotalVariants: config.maxTotalVariants ?? 100,
    });
  }, [config]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      await onConfigUpdate(formData);
      toast({
        title: 'Configuration Updated',
        description: 'Mock server configuration has been updated successfully.',
      });
    } catch (error) {
      console.error('Failed to update configuration:', error);
      const msg =
        error instanceof Error && error.message
          ? error.message
          : 'Failed to update configuration.';
      toast({
        title: 'Configuration not saved',
        description: msg,
        variant: 'destructive',
        duration: 20_000,
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleSyncOperations = async () => {
    if (!onSyncOperations) return;
    setIsSyncing(true);
    try {
      await onSyncOperations();
      toast({ title: 'Operations Synced', description: 'Operation configs synced from latest spec.' });
    } catch (error) {
      console.error('Failed to sync operations:', error);
      toast({ title: 'Sync Failed', description: 'Failed to sync operations from spec.', variant: 'destructive' });
    } finally {
      setIsSyncing(false);
    }
  };

  const handleReset = () => {
    if (config) {
      setFormData({
        defaultStrategy: config.defaultStrategy,
        validationMode: config.validationMode,
        maxLogEntries: config.maxLogEntries,
        rateLimitPerMinute: config.rateLimitPerMinute,
        maxVariantsPerOperation: config.maxVariantsPerOperation,
        maxTotalVariants: config.maxTotalVariants,
      });
    }
  };

  if (!config) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center py-12">
          <div className="text-center">
            <RefreshCw className="h-12 w-12 text-muted-foreground mx-auto mb-4 animate-spin" />
            <h3 className="text-lg font-semibold mb-2">Loading Configuration</h3>
            <p className="text-muted-foreground">Please wait while we load the mock server configuration.</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Mock Server Configuration</CardTitle>
              <CardDescription>
                Configure how your mock server behaves and responds to requests
              </CardDescription>
            </div>
            {onSyncOperations && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleSyncOperations}
                disabled={isSyncing}
              >
                <RotateCcw className={`h-4 w-4 mr-2 ${isSyncing ? 'animate-spin' : ''}`} />
                {isSyncing ? 'Syncing...' : 'Sync Operations'}
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Response Strategy */}
            <div className="space-y-2">
              <Label htmlFor="defaultStrategy">Default Response Strategy</Label>
              <Select
                value={formData.defaultStrategy}
                onValueChange={(value: any) => setFormData(prev => ({ ...prev, defaultStrategy: value }))}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="RANDOM">Random</SelectItem>
                  <SelectItem value="ROUND_ROBIN">Round Robin</SelectItem>
                  <SelectItem value="SEQUENTIAL">Sequential</SelectItem>
                  <SelectItem value="DEFAULT_ONLY">Default Only</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-sm text-muted-foreground">
                How to select responses when multiple variants exist for an operation
              </p>
            </div>

            {/* Validation Mode */}
            <div className="space-y-2">
              <Label htmlFor="validationMode">Schema validation</Label>
              <Select
                value={formData.validationMode}
                onValueChange={(value: any) => setFormData(prev => ({ ...prev, validationMode: value }))}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="OFF">Off</SelectItem>
                  <SelectItem value="WARN">Warn (log mismatches)</SelectItem>
                  <SelectItem value="STRICT">Strict (reject invalid JSON)</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-sm text-muted-foreground">
                Validates incoming JSON requests and static variant response bodies against the OpenAPI spec (CEL variants skip response validation).
              </p>
            </div>

            {/* Limits */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="maxLogEntries">Max Log Entries</Label>
                <Input
                  id="maxLogEntries"
                  type="number"
                  min="1"
                  max="10000"
                  value={formData.maxLogEntries}
                  onChange={(e) => setFormData(prev => ({ ...prev, maxLogEntries: parseInt(e.target.value) }))}
                />
                <p className="text-sm text-muted-foreground">
                  Maximum number of request/response logs to keep
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="rateLimitPerMinute">Rate Limit (per minute)</Label>
                <Input
                  id="rateLimitPerMinute"
                  type="number"
                  min="1"
                  max="10000"
                  value={formData.rateLimitPerMinute}
                  onChange={(e) => setFormData(prev => ({ ...prev, rateLimitPerMinute: parseInt(e.target.value) }))}
                />
                <p className="text-sm text-muted-foreground">
                  Maximum requests allowed per minute per client
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="maxVariantsPerOperation">Max Variants per Operation</Label>
                <Input
                  id="maxVariantsPerOperation"
                  type="number"
                  min="1"
                  max="100"
                  value={formData.maxVariantsPerOperation}
                  onChange={(e) => setFormData(prev => ({ ...prev, maxVariantsPerOperation: parseInt(e.target.value) }))}
                />
                <p className="text-sm text-muted-foreground">
                  Maximum response variants allowed per operation
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="maxTotalVariants">Max Total Variants</Label>
                <Input
                  id="maxTotalVariants"
                  type="number"
                  min="1"
                  max="1000"
                  value={formData.maxTotalVariants}
                  onChange={(e) => setFormData(prev => ({ ...prev, maxTotalVariants: parseInt(e.target.value) }))}
                />
                <p className="text-sm text-muted-foreground">
                  Maximum total response variants allowed for this mock server
                </p>
              </div>
            </div>

            {/* Info Alert */}
            <Alert>
              <AlertDescription>
                <strong>Configuration Tips:</strong>
                <ul className="mt-2 space-y-1 text-sm">
                  <li>• <strong>Random:</strong> Best for testing different scenarios</li>
                  <li>• <strong>Round Robin:</strong> Good for load testing</li>
                  <li>• <strong>Sequential:</strong> Useful for testing workflows</li>
                  <li>• <strong>Default Only:</strong> Always returns the default variant</li>
                </ul>
              </AlertDescription>
            </Alert>

            {/* Actions */}
            <div className="flex items-center justify-between pt-4 border-t">
              <Button
                type="button"
                variant="outline"
                onClick={handleReset}
                disabled={isLoading}
              >
                <RefreshCw className="h-4 w-4 mr-2" />
                Reset
              </Button>
              
              <Button type="submit" disabled={isLoading}>
                <Save className="h-4 w-4 mr-2" />
                {isLoading ? 'Saving...' : 'Save Configuration'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {/* Current Configuration Summary */}
      <Card>
        <CardHeader>
          <CardTitle>Current Configuration</CardTitle>
          <CardDescription>
            Summary of your current mock server configuration
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm font-medium">Response Strategy</span>
                <span className="text-sm text-muted-foreground">{config.defaultStrategy}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm font-medium">Validation Mode</span>
                <span className="text-sm text-muted-foreground">{config.validationMode}</span>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm font-medium">Max Log Entries</span>
                <span className="text-sm text-muted-foreground">{config.maxLogEntries}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm font-medium">Rate Limit</span>
                <span className="text-sm text-muted-foreground">{config.rateLimitPerMinute}/min</span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
