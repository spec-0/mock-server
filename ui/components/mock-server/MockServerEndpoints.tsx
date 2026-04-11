'use client';

import React, { useState, useEffect, useMemo } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { PanelLeftOpen, Settings, X } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { MockServer, MockResponseVariant, MockOperationConfig, GeneratedRequest } from '@/services/mockServerAdminClient';
import { Operation } from '@/lib/api/types';
import MockServerOperationPanel from './MockServerOperationPanel';
import { useLoading } from '@/hooks/useLoading';
import OperationTestPanel from './endpoints/OperationTestPanel';
import VariantList from './endpoints/VariantList';
import VariantCreateDialog from './endpoints/VariantCreateDialog';
import VariantEditDialog from './endpoints/VariantEditDialog';
import VariantDetailDialog from './endpoints/VariantDetailDialog';
import { buildMockTestCurl } from '@/lib/build-mock-test-curl';

interface MockServerEndpointsProps {
  /** REST API origin (same as standalone client); used for cURL that matches “Send”. */
  apiBaseUrl: string;
  mockServer: MockServer;
  variants: MockResponseVariant[];
  operations: Operation[];
  operationConfigs: MockOperationConfig[];
  onVariantCreate: (variant: any) => Promise<void>;
  onVariantUpdate: (variantId: string, updates: any) => Promise<void>;
  onVariantDelete: (variantId: string) => Promise<void>;
  onOperationToggle: (operationId: string, enabled: boolean) => Promise<void>;
  onStrategyChange?: (operationId: string, strategy: string) => Promise<void>;
  onTestEndpoint: (path: string, method: string, body?: any, headers?: Record<string, string>, operationId?: string) => Promise<{
    data: any;
    status: number;
    statusText: string;
    headers: Record<string, string>;
    isMockResponse: boolean;
    mockVariantId?: string;
    mockOperationId?: string;
  }>;
  onGenerateRequest: (operationId: string) => Promise<GeneratedRequest>;
}

export default function MockServerEndpoints({
  apiBaseUrl,
  mockServer,
  variants,
  operations,
  operationConfigs,
  onVariantCreate,
  onVariantUpdate,
  onVariantDelete,
  onOperationToggle,
  onStrategyChange,
  onTestEndpoint,
  onGenerateRequest,
}: MockServerEndpointsProps) {
  const { toast } = useToast();
  const { isLoading: isTesting, withLoading: withTesting } = useLoading();
  const [selectedOperation, setSelectedOperation] = useState<Operation | null>(null);
  const [selectedOperationConfig, setSelectedOperationConfig] = useState<MockOperationConfig | null>(null);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [editingVariant, setEditingVariant] = useState<MockResponseVariant | null>(null);

  // Request/Response testing state
  const [requestBody, setRequestBody] = useState('');
  const [headerRows, setHeaderRows] = useState<Array<{key: string, value: string}>>([
    { key: '', value: '' }
  ]);
  const [testResult, setTestResult] = useState<any>(null);
  const [generatedRequest, setGeneratedRequest] = useState<GeneratedRequest | null>(null);
  const [expandedVariants, setExpandedVariants] = useState<Set<string>>(new Set());
  const [detailVariant, setDetailVariant] = useState<MockResponseVariant | null>(null);
  const [isOperationsPanelCollapsed, setIsOperationsPanelCollapsed] = useState(false);
  const [pathParams, setPathParams] = useState<Record<string, string>>({});

  // Group variants by operation
  const variantsByOperation = useMemo(() => {
    return variants.reduce((acc, variant) => {
      if (variant.operationId && !acc[variant.operationId]) {
        acc[variant.operationId] = [];
      }
      if (variant.operationId) {
        acc[variant.operationId].push(variant);
      }
      return acc;
    }, {} as Record<string, MockResponseVariant[]>);
  }, [variants]);

  // Create variants count map
  const variantsCount = useMemo(() => {
    const count: Record<string, number> = {};
    variants.forEach(variant => {
      if (variant.operationId) {
        count[variant.operationId] = (count[variant.operationId] || 0) + 1;
      }
    });
    return count;
  }, [variants]);

  // Handle operation selection
  const handleOperationSelect = async (operationId: string) => {
    const operation = operations.find(op => op.operationId === operationId);
    if (operation) {
      setSelectedOperation(operation);

      // Find the operation config
      const config = operationConfigs.find(c => c.operationId === operationId);
      setSelectedOperationConfig(config || null);

      // Reset testing state
      setRequestBody('');
      setTestResult(null);
      setGeneratedRequest(null);
      setPathParams({});

      // Generate initial request if it's a POST/PUT/PATCH operation
      if (['POST', 'PUT', 'PATCH'].includes(operation.method || '')) {
        try {
          const generated = await onGenerateRequest(operationId);
          setGeneratedRequest(generated);
          setRequestBody(generated.requestBody || '');
        } catch (error) {
          console.warn('Failed to generate request:', error);
        }
      }
    }
  };

  const toggleVariantExpansion = (variantId: string) => {
    setExpandedVariants(prev => {
      const newSet = new Set(prev);
      if (newSet.has(variantId)) {
        newSet.delete(variantId);
      } else {
        newSet.add(variantId);
      }
      return newSet;
    });
  };

  // Header management helpers
  const getHeadersAsJson = () => {
    const headers: Record<string, string> = {};
    headerRows.forEach(row => {
      if (row.key.trim() && row.value.trim()) {
        headers[row.key.trim()] = row.value.trim();
      }
    });
    return headers;
  };

  const beautifyJson = () => {
    if (!requestBody.trim()) {
      toast({
        title: 'No JSON to format',
        description: 'Please enter some JSON content first.',
        variant: 'destructive',
      });
      return;
    }

    try {
      const parsed = JSON.parse(requestBody);
      const beautified = JSON.stringify(parsed, null, 2);
      setRequestBody(beautified);
      toast({
        title: 'JSON Formatted',
        description: 'Request body has been beautified successfully.',
      });
    } catch (error) {
      toast({
        title: 'Invalid JSON',
        description: 'Please check your JSON syntax and try again.',
        variant: 'destructive',
      });
    }
  };

  const handleCreateVariant = async (data: {
    operationId: string;
    responseName: string;
    statusCode: string;
    responseBody: string;
    headers: string;
    isDefault: boolean;
    displayOrder: number;
    celExpression?: string | null;
  }) => {
    // Parse and validate headers JSON, then stringify it to send as a string to backend
    let headersString: string | undefined;
    if (data.headers && data.headers.trim()) {
      try {
        const parsedHeaders = JSON.parse(data.headers.trim());
        headersString = JSON.stringify(parsedHeaders);
      } catch (error) {
        toast({
          title: 'Invalid Headers JSON',
          description: 'Please enter valid JSON for headers.',
          variant: 'destructive',
        });
        return;
      }
    }

    const variant = {
      operationId: data.operationId,
      responseName: data.responseName,
      statusCode: data.statusCode,
      responseBody: data.responseBody,
      headers: headersString,
      isDefault: data.isDefault,
      displayOrder: data.displayOrder,
      celExpression: data.celExpression ?? null,
    };

    try {
      await onVariantCreate(variant);
      setIsCreateDialogOpen(false);
    } catch (error) {
      const msg =
        error instanceof Error && error.message
          ? error.message
          : 'Could not create this variant.';
      toast({
        title: 'Variant not saved',
        description: msg,
        variant: 'destructive',
        duration: 30_000,
      });
    }
  };

  const handleUpdateVariant = async (variantId: string, data: {
    responseName: string;
    statusCode: string;
    responseBody: string;
    headers: string;
    isDefault: boolean;
    displayOrder: number;
    celExpression?: string | null;
  }) => {
    if (!editingVariant) return;

    // Parse and validate headers JSON, then stringify it to send as a string to backend
    let headersString: string | undefined;
    if (data.headers && data.headers.trim()) {
      try {
        const parsedHeaders = JSON.parse(data.headers.trim());
        headersString = JSON.stringify(parsedHeaders);
      } catch (error) {
        toast({
          title: 'Invalid Headers JSON',
          description: 'Please enter valid JSON for headers.',
          variant: 'destructive',
        });
        return;
      }
    }

    const updates = {
      operationId: editingVariant.operationId,
      responseName: data.responseName,
      statusCode: data.statusCode,
      responseBody: data.responseBody,
      headers: headersString,
      isDefault: data.isDefault,
      displayOrder: data.displayOrder,
      celExpression: data.celExpression ?? null,
    };

    try {
      await onVariantUpdate(variantId, updates);
      setEditingVariant(null);
    } catch (error) {
      const msg =
        error instanceof Error && error.message
          ? error.message
          : 'Could not update this variant.';
      toast({
        title: 'Variant not saved',
        description: msg,
        variant: 'destructive',
        duration: 30_000,
      });
    }
  };

  /**
   * Path segment after /mock/{mockServerId} — must match what MockServerStandaloneClient
   * appends: `${base}/mock/${id}${thisPath}`. Not the full /mock/id/... prefix.
   */
  const buildMockRelativePath = (params: Record<string, string>): string => {
    if (!selectedOperation?.path) return '/';
    let p = selectedOperation.path;
    Object.entries(params).forEach(([key, value]) => {
      p = p.replace(`{${key}}`, value);
    });
    return p.startsWith('/') ? p : `/${p}`;
  };

  const absoluteMockRequestUrl = (): string => {
    const base = apiBaseUrl.replace(/\/$/, '');
    return `${base}/mock/${mockServer.mockServerId}${buildMockRelativePath(pathParams)}`;
  };

  const handleTestEndpoint = async () => {
    if (!selectedOperation?.path) return;

    await withTesting(async () => {
      const body = requestBody ? JSON.parse(requestBody) : undefined;
      const headers = getHeadersAsJson();

      const result = await onTestEndpoint(
        buildMockRelativePath(pathParams),
        selectedOperation.method || 'GET',
        body,
        headers,
        selectedOperation.operationId,
      );

      if (result.isMockResponse) {
        setTestResult({
          status: result.status,
          statusText: result.statusText,
          data: result.data,
          headers: result.headers,
          mockVariantId: result.mockVariantId,
          mockOperationId: result.mockOperationId,
        });

        if (result.status >= 400) {
          const d = result.data as { message?: string; error?: string; details?: string[] } | null;
          const lines = [d?.message, ...(Array.isArray(d?.details) ? d.details : [])].filter(
            (x): x is string => typeof x === 'string' && x.length > 0,
          );
          const desc =
            lines.length > 0 ? lines.join('\n') : (d?.error ?? `HTTP ${result.status}`);
          toast({
            title: 'Mock request rejected',
            description: desc,
            variant: 'destructive',
            duration: 30_000,
          });
        } else {
          toast({
            title: 'Test Successful',
            description: `Mock response returned with status ${result.status}${result.mockVariantId ? ` (Variant: ${result.mockVariantId})` : ''}`,
            duration: 5000,
          });
        }
      } else {
        setTestResult({
          status: result.status,
          statusText: result.statusText,
          data: result.data,
          headers: result.headers,
        });

        toast({
          title: 'Test Completed',
          description: `Response received with status ${result.status}`,
          duration: 5000,
        });
      }
    }, 'Testing Endpoint...');
  };

  const copyEndpointUrl = () => {
    if (!selectedOperation?.path) return;
    void navigator.clipboard.writeText(absoluteMockRequestUrl());
    toast({
      title: 'URL Copied',
      description: 'Full mock URL (same as cURL) with current path parameters.',
    });
  };

  const copyEndpointCurl = () => {
    if (!selectedOperation?.path) return;
    try {
      const pathForFetch = buildMockRelativePath(pathParams);
      let requestBodyRaw: string | undefined;
      if (['POST', 'PUT', 'PATCH', 'DELETE'].includes((selectedOperation.method || 'GET').toUpperCase()) && requestBody.trim()) {
        JSON.parse(requestBody);
        requestBodyRaw = requestBody;
      }
      const cmd = buildMockTestCurl({
        apiBaseUrl,
        mockServerId: mockServer.mockServerId,
        pathForFetch,
        method: selectedOperation.method || 'GET',
        customHeaders: getHeadersAsJson(),
        operationId: selectedOperation.operationId || undefined,
        requestBodyRaw,
      });
      void navigator.clipboard.writeText(cmd);
      toast({
        title: 'cURL copied',
        description: 'Uses the same URL and headers as Test Endpoint (including X-Mock-Operation-Id when set).',
      });
    } catch {
      toast({
        title: 'Cannot build cURL',
        description: 'Fix request body JSON, or clear it for methods without a body.',
        variant: 'destructive',
      });
    }
  };

  const regenerateRequest = async () => {
    if (!selectedOperation?.operationId) return;

    try {
      const generated = await onGenerateRequest(selectedOperation.operationId);
      setGeneratedRequest(generated);
      setRequestBody(generated.requestBody || '');
    } catch (error) {
      toast({
        title: 'Generation Failed',
        description: 'Failed to generate request body.',
        variant: 'destructive',
      });
    }
  };

  // Get available status codes for the selected operation based on HTTP method
  const getAvailableStatusCodes = () => {
    const method = (selectedOperation?.method || 'GET').toUpperCase();
    const common = ['400', '401', '403', '404', '422', '429', '500', '503'];
    switch (method) {
      case 'GET':
        return ['200', '206', ...common];
      case 'POST':
        return ['200', '201', '202', ...common];
      case 'PUT':
      case 'PATCH':
        return ['200', '204', ...common];
      case 'DELETE':
        return ['200', '204', ...common];
      default:
        return ['200', '201', '204', ...common];
    }
  };

  // Show disabled state when mock server is disabled
  if (!mockServer.isEnabled) {
    return (
      <div className="flex items-center justify-center h-full">
        <Card className="w-full max-w-md">
          <CardContent className="flex flex-col items-center justify-center p-8 text-center space-y-4">
            <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center">
              <X className="h-8 w-8 text-red-600" />
            </div>
            <div className="space-y-2">
              <h3 className="text-lg font-semibold text-gray-900">Mock Server Disabled</h3>
              <p className="text-sm text-gray-600">
                The mock server is currently disabled. Enable it in the Settings tab to test endpoints and manage response variants.
              </p>
            </div>
            <Alert className="w-full">
              <AlertDescription className="text-sm">
                <strong>Note:</strong> When disabled, the mock server will not respond to any requests.
                Enable the mock server to start serving mock responses.
              </AlertDescription>
            </Alert>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex min-h-[min(560px,calc(100vh-12rem))] gap-0">
      {/* Left: full operations panel or slim rail (no floating overlay) */}
      {isOperationsPanelCollapsed ? (
        <aside
          className="flex shrink-0 w-12 flex-col border-r border-border bg-muted/50"
          aria-label="Operations sidebar"
        >
          <TooltipProvider delayDuration={300}>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-12 w-12 shrink-0 rounded-none border-b border-border/80 hover:bg-muted"
                  onClick={() => setIsOperationsPanelCollapsed(false)}
                  aria-expanded={false}
                  aria-controls="operations-sidebar-panel"
                >
                  <PanelLeftOpen className="h-5 w-5" aria-hidden />
                </Button>
              </TooltipTrigger>
              <TooltipContent side="right" className="max-w-[220px]">
                <p className="font-medium">Show operations</p>
                <p className="text-xs text-muted-foreground mt-0.5">Open the API operations list and search</p>
              </TooltipContent>
            </Tooltip>
          </TooltipProvider>
          <div className="flex flex-1 flex-col items-center pt-3 pb-2">
            <span
              className="select-none text-[10px] font-semibold uppercase tracking-[0.2em] text-muted-foreground leading-none"
              style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}
            >
              Operations
            </span>
          </div>
        </aside>
      ) : (
        <aside
          id="operations-sidebar-panel"
          className="flex min-h-0 min-w-[260px] w-[min(400px,38vw)] max-w-lg shrink-0 flex-col transition-[width] duration-200 ease-out"
        >
          <MockServerOperationPanel
            operations={operations}
            operationConfigs={operationConfigs}
            selectedOperationId={selectedOperation?.operationId}
            onOperationSelect={handleOperationSelect}
            onOperationToggle={onOperationToggle}
            onStrategyChange={onStrategyChange}
            variantsCount={variantsCount}
            onCollapseSidebar={() => setIsOperationsPanelCollapsed(true)}
          />
        </aside>
      )}

      {/* Main: operation detail, test, variants */}
      <div className="min-w-0 flex-1 overflow-auto pl-4">
        {selectedOperation ? (
          <div className="space-y-6">
            {/* Operation Header */}
            <Card className="border-primary-200 bg-gradient-to-br from-primary-50/30 to-secondary-50/30">
              <CardHeader className="bg-gradient-to-r from-primary-500 to-secondary-600 text-white rounded-t-lg">
                <CardTitle className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Badge className="text-xs bg-white/20 text-white border-white/30">
                      {selectedOperation.method}
                    </Badge>
                    <code className="text-sm font-mono text-primary-100">{selectedOperation.path}</code>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Badge variant="outline" className="bg-white/20 text-white border-white/30">
                      {selectedOperation.operationId ? (variantsCount[selectedOperation.operationId] || 0) : 0} variants
                    </Badge>
                    {selectedOperationConfig?.isEnabled ? (
                      <Badge className="bg-green-100 text-green-800">Enabled</Badge>
                    ) : (
                      <Badge className="bg-red-100 text-red-800">Disabled</Badge>
                    )}
                  </div>
                </CardTitle>
                <CardDescription className="text-primary-100">
                  {selectedOperation.operationId}
                </CardDescription>
              </CardHeader>
            </Card>

            {/* Request/Response Testing Panel */}
            <OperationTestPanel
              selectedOperation={selectedOperation}
              pathParams={pathParams}
              onPathParamsChange={setPathParams}
              requestBody={requestBody}
              onRequestBodyChange={setRequestBody}
              headerRows={headerRows}
              onHeaderRowsChange={setHeaderRows}
              testResult={testResult}
              isTesting={isTesting}
              onTest={handleTestEndpoint}
              onCopyUrl={copyEndpointUrl}
              onCopyCurl={copyEndpointCurl}
              onBeautifyJson={beautifyJson}
              onRegenerateRequest={regenerateRequest}
            />

            {/* Response Variants */}
            <VariantList
              operationId={selectedOperation.operationId || ''}
              variants={selectedOperation.operationId ? (variantsByOperation[selectedOperation.operationId] || []) : []}
              expandedVariants={expandedVariants}
              onToggleExpand={toggleVariantExpansion}
              onViewDetail={setDetailVariant}
              onEdit={setEditingVariant}
              onDelete={onVariantDelete}
              onCreateClick={() => setIsCreateDialogOpen(true)}
              mockServer={mockServer}
              selectedOperationPath={selectedOperation.path}
            />

            {/* Create Variant Dialog */}
            <VariantCreateDialog
              isOpen={isCreateDialogOpen}
              onOpenChange={setIsCreateDialogOpen}
              operationId={selectedOperation.operationId || ''}
              availableStatusCodes={getAvailableStatusCodes()}
              onSubmit={handleCreateVariant}
            />

            {/* Edit Variant Dialog */}
            <VariantEditDialog
              variant={editingVariant}
              onClose={() => setEditingVariant(null)}
              availableStatusCodes={getAvailableStatusCodes()}
              onSubmit={handleUpdateVariant}
            />

            <VariantDetailDialog
              variant={detailVariant}
              onClose={() => setDetailVariant(null)}
              onEdit={(v) => setEditingVariant(v)}
            />
          </div>
        ) : (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-12">
              <Settings className="h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold mb-2">Select an Operation</h3>
              <p className="text-muted-foreground text-center">
                {isOperationsPanelCollapsed
                  ? 'Expand the Operations strip on the left, then pick an operation.'
                  : 'Choose an operation from the sidebar to view and manage mock responses.'}
              </p>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
