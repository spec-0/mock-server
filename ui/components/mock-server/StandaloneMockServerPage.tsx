'use client';

/**
 * StandaloneMockServerPage
 *
 * Thin wrapper around the shared mock server tab components that wires
 * them to MockServerStandaloneClient instead of MockServerAdminClient.
 * No Clerk auth, no org/team context.
 */

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowLeft, ExternalLink, Activity, FileText, BarChart3, Settings, Pause, Play, Zap } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useToast } from '@/hooks/use-toast';
import { FeedbackState } from '@/components/ui/feedback-state';

import { useStandaloneClient, useStandaloneBaseUrl } from '@/context/StandaloneServerContext';
import type {
  MockServer,
  MockServerConfig,
  MockResponseVariant,
  MockRequestLog,
  MockOperationConfig,
  MockServerEnvVar,
  MockServerMcpConfig,
  GeneratedRequest,
  DashboardData,
} from '@/services/mockServerAdminClient';
import type { Operation } from '@/lib/api/types';

import MockServerOverview from './MockServerOverview';
import MockServerEndpoints from './MockServerEndpoints';
import MockServerConfiguration from './MockServerConfiguration';
import MockServerLogs from './MockServerLogs';
import MockServerAnalyticsComponent from './MockServerAnalytics';
import MockServerSettings from './MockServerSettings';

interface Props {
  mockServerId: string;
}

export default function StandaloneMockServerPage({ mockServerId }: Props) {
  const router = useRouter();
  const { toast } = useToast();
  const client = useStandaloneClient();
  const baseUrl = useStandaloneBaseUrl();

  const [mockServer, setMockServer] = useState<MockServer | null>(null);
  const [config, setConfig] = useState<MockServerConfig | null>(null);
  const [variants, setVariants] = useState<MockResponseVariant[]>([]);
  const [logs, setLogs] = useState<MockRequestLog[]>([]);
  const [operationConfigs, setOperationConfigs] = useState<MockOperationConfig[]>([]);
  const [specOperations, setSpecOperations] = useState<Operation[]>([]);
  const [mcpConfig, setMcpConfig] = useState<MockServerMcpConfig | null>(null);
  const [envVars, setEnvVars] = useState<MockServerEnvVar[]>([]);
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('overview');
  const [isToggling, setIsToggling] = useState(false);

  const hasLoaded = useRef(false);

  const toDateInputValue = (d: Date) => {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  const adjustDateInput = (dateStr: string, deltaDays: number) => {
    const [y, m, d] = dateStr.split('-').map(Number);
    const dt = new Date(y, m - 1, d);
    dt.setDate(dt.getDate() + deltaDays);
    return toDateInputValue(dt);
  };

  const loadOverviewDashboardData = useCallback(async (ms: MockServer) => {
    const id = ms.mockServerId;
    if (!id) return;
    const today = toDateInputValue(new Date());
    const allTimeFrom = '2000-01-01';
    const adjustedFrom = adjustDateInput(allTimeFrom, -1);
    const adjustedTo = adjustDateInput(today, 1);
    try {
      const analytics: any = await client.getAnalytics(id, adjustedFrom, adjustedTo);
      const totalRequests = Number(analytics?.totalRequests ?? 0);
      const errorRateRaw = Number(analytics?.errorRate ?? 0);
      const errorRateFraction = errorRateRaw > 1 ? errorRateRaw / 100 : errorRateRaw;
      const failedRequests = Math.max(0, Math.round(totalRequests * errorRateFraction));
      const successfulRequests = Math.max(0, totalRequests - failedRequests);
      const avgResponseTime = Number(analytics?.averageResponseTime ?? 0);
      setDashboardData({
        mockServer: ms,
        recentAnalytics: [],
        summary: {
          totalRequests,
          successfulRequests,
          failedRequests,
          avgResponseTime,
          uniqueOperations: Number(analytics?.uniqueOperations ?? 0),
        },
      });
    } catch {
      setDashboardData(null);
    }
  }, [client]);

  useEffect(() => {
    if (hasLoaded.current) return;
    hasLoaded.current = true;

    const load = async () => {
      try {
        setLoading(true);
        const ms = await client.getMockServer(mockServerId);
        setMockServer(ms);
        void loadOverviewDashboardData(ms);

        const [cfgRes, varRes, logsRes, opRes, specOpsRes, mcpRes, envRes] = await Promise.allSettled([
          client.getMockServerConfig(mockServerId),
          client.getVariants(mockServerId),
          client.getLogs(mockServerId, 50),
          client.getOperationConfigs(mockServerId),
          client.getSpecOperations(mockServerId),
          client.getMcpConfig(mockServerId),
          client.getEnvVars(mockServerId),
        ]);

        if (cfgRes.status === 'fulfilled') setConfig(cfgRes.value);
        if (varRes.status === 'fulfilled') setVariants(varRes.value);
        if (logsRes.status === 'fulfilled') setLogs(logsRes.value);
        if (opRes.status === 'fulfilled') setOperationConfigs(opRes.value);
        if (specOpsRes.status === 'fulfilled') setSpecOperations(specOpsRes.value);
        if (mcpRes.status === 'fulfilled') setMcpConfig(mcpRes.value);
        if (envRes.status === 'fulfilled') setEnvVars(envRes.value);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load mock server.');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [client, mockServerId, loadOverviewDashboardData]);

  const handleToggle = async (enabled: boolean) => {
    if (!mockServer) return;
    setIsToggling(true);
    try {
      const updated = await client.performMockServerAction(mockServerId, enabled ? 'enable' : 'disable');
      setMockServer(updated);
      void loadOverviewDashboardData(updated);
      toast({ title: enabled ? 'Mock Server Enabled' : 'Mock Server Disabled' });
    } catch (err) {
      toast({ title: 'Error', description: err instanceof Error ? err.message : 'Failed to update.', variant: 'destructive' });
    } finally {
      setIsToggling(false);
    }
  };

  const handleConfigUpdate = async (newConfig: MockServerConfig) => {
    const updated = await client.updateMockServerConfig(mockServerId, newConfig);
    setConfig(updated);
    toast({ title: 'Configuration Updated' });
  };

  const handleVariantCreate = async (variant: any) => {
    const created = await client.createVariant(mockServerId, variant);
    setVariants(prev => [...prev, created]);
    toast({ title: 'Variant Created' });
  };

  const handleVariantUpdate = async (variantId: string, updates: any) => {
    const updated = await client.updateVariant(mockServerId, variantId, updates);
    setVariants(prev => prev.map(v => v.variantId === variantId ? updated : v));
    toast({ title: 'Variant Updated' });
  };

  const handleVariantDelete = async (variantId: string) => {
    try {
      await client.deleteVariant(mockServerId, variantId);
      setVariants(prev => prev.filter(v => v.variantId !== variantId));
      toast({ title: 'Variant Deleted' });
    } catch (err) {
      toast({
        title: 'Could not delete variant',
        description: err instanceof Error ? err.message : 'Delete failed.',
        variant: 'destructive',
        duration: 15_000,
      });
    }
  };

  const handleOperationToggle = async (operationId: string, enabled: boolean) => {
    await client.toggleOperation(mockServerId, operationId, enabled);
    setOperationConfigs(prev =>
      prev.map(c => c.operationId === operationId ? { ...c, isEnabled: enabled } : c)
    );
  };

  const handleOperationStrategyChange = async (operationId: string, strategy: string) => {
    const updated = await client.updateOperationConfig(mockServerId, operationId, {
      responseStrategy: strategy,
    });
    setOperationConfigs(prev =>
      prev.map(c => c.operationId === operationId ? { ...c, responseStrategy: updated.responseStrategy } : c)
    );
  };

  const handleGenerateRequest = async (operationId: string): Promise<GeneratedRequest> =>
    client.generateRequest(mockServerId, operationId);

  const handleTestEndpoint = async (
    path: string, method: string, body?: any,
    headers?: Record<string, string>, operationId?: string,
  ) => client.testEndpointWithHeaders(mockServerId, path, method, body, headers, operationId);

  const handleDelete = async () => {
    await client.deleteMockServer(mockServerId);
    router.push('/');
  };

  const handleUpdateName = async (updates: { mockServerName: string; description?: string }) => {
    const updated = await client.updateMockServer(mockServerId, { name: updates.mockServerName });
    setMockServer(updated);
  };

  const handleMcpToggle = async (enabled: boolean) => {
    const updated = await client.updateMcpConfig(mockServerId, enabled);
    setMcpConfig(updated);
  };

  const handleEnvVarCreate = async (key: string, value: string) => {
    const created = await client.createEnvVar(mockServerId, key, value);
    setEnvVars(prev => [...prev, created]);
  };

  const handleEnvVarDelete = async (envVarId: string) => {
    await client.deleteEnvVar(mockServerId, envVarId);
    setEnvVars(prev => prev.filter(v => v.envVarId !== envVarId));
  };

  const handleRefresh = async () => {
    const [fresh, freshLogs] = await Promise.allSettled([
      client.getMockServer(mockServerId),
      client.getLogs(mockServerId, 50),
    ]);
    if (fresh.status === 'fulfilled') {
      setMockServer(fresh.value);
      void loadOverviewDashboardData(fresh.value);
    }
    if (freshLogs.status === 'fulfilled') setLogs(freshLogs.value);
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="sm" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <Skeleton className="h-8 w-64" />
        </div>
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (error) {
    return (
      <FeedbackState
        type="error"
        title="Could not load mock server"
        description={error}
        actionLabel="Try again"
        onAction={() => window.location.reload()}
      />
    );
  }

  if (!mockServer) {
    return (
      <FeedbackState
        type="empty"
        title="Mock server not found"
        description="This mock server may have been deleted."
        actionLabel="Back to list"
        onAction={() => router.push('/')}
      />
    );
  }

  const mockBaseUrl = `${baseUrl}/mock/${mockServerId}`;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-gradient-to-r from-indigo-500 to-purple-600 rounded-lg p-6 text-white">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => router.push('/')}
              className="text-white hover:bg-white/20"
            >
              <ArrowLeft className="h-4 w-4 mr-1" />
              Back
            </Button>
            <div className="flex items-center gap-3">
              <div className="p-2 bg-white/20 rounded-lg">
                <Zap className="h-6 w-6" />
              </div>
              <div>
                <h1 className="text-2xl font-bold">{mockServer.mockServerName}</h1>
                <p className="text-indigo-100 text-sm font-mono mt-1 select-all">{mockBaseUrl}</p>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <Badge className={mockServer.isEnabled ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}>
              {mockServer.isEnabled ? 'Enabled' : 'Disabled'}
            </Badge>
            <Button
              variant={mockServer.isEnabled ? 'destructive' : 'default'}
              size="sm"
              onClick={() => handleToggle(!mockServer.isEnabled)}
              disabled={isToggling}
              className={mockServer.isEnabled
                ? 'bg-red-500 hover:bg-red-600'
                : 'bg-white/20 hover:bg-white/30 text-white border-white/30'}
            >
              {isToggling
                ? <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
                : mockServer.isEnabled
                  ? <><Pause className="h-4 w-4 mr-1" />Disable</>
                  : <><Play className="h-4 w-4 mr-1" />Enable</>
              }
            </Button>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-4">
        <TabsList className="grid w-full grid-cols-3 sm:grid-cols-6 h-auto gap-1">
          <TabsTrigger value="overview" className="flex items-center gap-2">
            <Activity className="h-4 w-4" /><span>Overview</span>
          </TabsTrigger>
          <TabsTrigger value="endpoints" className="flex items-center gap-2" disabled={!mockServer.isEnabled}>
            <ExternalLink className="h-4 w-4" /><span>Endpoints</span>
          </TabsTrigger>
          <TabsTrigger value="configuration" className="flex items-center gap-2">
            <Settings className="h-4 w-4" /><span>Configuration</span>
          </TabsTrigger>
          <TabsTrigger value="logs" className="flex items-center gap-2">
            <FileText className="h-4 w-4" /><span>Logs</span>
          </TabsTrigger>
          <TabsTrigger value="analytics" className="flex items-center gap-2">
            <BarChart3 className="h-4 w-4" /><span>Analytics</span>
          </TabsTrigger>
          <TabsTrigger value="settings" className="flex items-center gap-2">
            <Settings className="h-4 w-4" /><span>Settings</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="overview">
          <MockServerOverview
            mockServer={mockServer}
            dashboardData={dashboardData}
            onRefresh={handleRefresh}
          />
        </TabsContent>

        <TabsContent value="endpoints">
          <MockServerEndpoints
            apiBaseUrl={baseUrl}
            mockServer={mockServer}
            variants={variants}
            operations={specOperations}
            operationConfigs={operationConfigs}
            onVariantCreate={handleVariantCreate}
            onVariantUpdate={handleVariantUpdate}
            onVariantDelete={handleVariantDelete}
            onOperationToggle={handleOperationToggle}
            onStrategyChange={handleOperationStrategyChange}
            onTestEndpoint={handleTestEndpoint}
            onGenerateRequest={handleGenerateRequest}
          />
        </TabsContent>

        <TabsContent value="configuration">
          <MockServerConfiguration
            config={config}
            onConfigUpdate={handleConfigUpdate}
          />
        </TabsContent>

        <TabsContent value="logs">
          <MockServerLogs
            mockServerId={mockServerId}
            apiClient={client as any}
            onRefresh={handleRefresh}
          />
        </TabsContent>

        <TabsContent value="analytics">
          <MockServerAnalyticsComponent mockServerId={mockServerId} apiClient={client as any} />
        </TabsContent>

        <TabsContent value="settings">
          <MockServerSettings
            mockServer={mockServer}
            onMockServerDelete={handleDelete}
            onMockServerUpdate={handleUpdateName}
            onToggleEnabled={handleToggle}
            mcpEnabled={mcpConfig?.mcpEnabled}
            mcpUrl={mcpConfig !== null ? `${baseUrl}/mcp/sse` : undefined}
            onMcpToggle={handleMcpToggle}
            envVars={envVars}
            onEnvVarCreate={handleEnvVarCreate}
            onEnvVarDelete={handleEnvVarDelete}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}
