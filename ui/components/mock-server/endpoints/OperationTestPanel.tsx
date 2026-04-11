'use client';

import React, { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';
import { Play, Copy, Upload, ChevronDown, ChevronRight, Code, Zap, Sparkles, Plus, X, Terminal } from 'lucide-react';
import { Operation } from '@/lib/api/types';
import PathVariableEditor from '../PathVariableEditor';

interface OperationTestPanelProps {
  selectedOperation: Operation;
  pathParams: Record<string, string>;
  onPathParamsChange: (params: Record<string, string>) => void;
  requestBody: string;
  onRequestBodyChange: (body: string) => void;
  headerRows: Array<{ key: string; value: string }>;
  onHeaderRowsChange: (rows: Array<{ key: string; value: string }>) => void;
  testResult: any;
  isTesting: boolean;
  onTest: () => Promise<void>;
  onCopyUrl: () => void;
  /** Same request the UI sends (absolute mock URL, headers, optional body). */
  onCopyCurl: () => void;
  onBeautifyJson: () => void;
  onRegenerateRequest: () => Promise<void>;
}

export default function OperationTestPanel({
  selectedOperation,
  pathParams,
  onPathParamsChange,
  requestBody,
  onRequestBodyChange,
  headerRows,
  onHeaderRowsChange,
  testResult,
  isTesting,
  onTest,
  onCopyUrl,
  onCopyCurl,
  onBeautifyJson,
  onRegenerateRequest,
}: OperationTestPanelProps) {
  const [isRequestHeadersExpanded, setIsRequestHeadersExpanded] = useState(false);
  const [isResponseHeadersExpanded, setIsResponseHeadersExpanded] = useState(false);

  const addHeaderRow = () => {
    onHeaderRowsChange([...headerRows, { key: '', value: '' }]);
  };

  const removeHeaderRow = (index: number) => {
    if (headerRows.length > 1) {
      onHeaderRowsChange(headerRows.filter((_, i) => i !== index));
    }
  };

  const updateHeaderRow = (index: number, field: 'key' | 'value', value: string) => {
    const updated = [...headerRows];
    updated[index][field] = value;
    onHeaderRowsChange(updated);
  };

  return (
    <div className="w-full overflow-x-auto">
      <div className="grid grid-cols-2 gap-4 min-w-[800px] items-start">
        {/* Left Panel - Request */}
        <Card className="flex flex-col border-primary-200 bg-gradient-to-br from-primary-50/30 to-secondary-50/30 h-full">
          <CardHeader className="pb-3 flex-shrink-0 h-[88px]">
            <CardTitle className="text-lg text-primary-700 flex items-center space-x-2 h-[28px]">
              <Zap className="h-5 w-5" />
              <span>Request</span>
            </CardTitle>
            <CardDescription className="text-primary-600 h-[20px]">
              Configure request data and test the mock endpoint
            </CardDescription>
          </CardHeader>
          <CardContent className="flex-1 flex flex-col gap-4 p-6">
            {/* Row 1: Generated URL with Path Variable Editor - Aligns with Response Status */}
            <div className="flex-shrink-0">
              <Label className="text-sm font-medium text-primary-700 mb-2 h-[20px] flex items-center">Mock URL</Label>
              <div className="flex items-center gap-2 p-3 bg-primary-50/50 border border-primary-200 rounded-lg">
                <div className="flex-1 min-w-0">
                  <PathVariableEditor
                    path={selectedOperation.path || ''}
                    pathParams={pathParams}
                    onPathParamsChange={onPathParamsChange}
                    showVariablesSection={false}
                    className=""
                  />
                </div>
                <div className="flex items-center gap-1 flex-shrink-0">
                  <TooltipProvider>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Button
                          variant="outline"
                          size="icon"
                          onClick={onCopyUrl}
                          aria-label="Copy mock URL"
                          className="border-primary-200 hover:bg-primary-50 dark:border-border dark:hover:bg-muted h-8 w-8"
                        >
                          <Copy className="h-4 w-4" aria-hidden="true" />
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent>Copy full mock URL (with path parameters)</TooltipContent>
                    </Tooltip>
                  </TooltipProvider>
                  <TooltipProvider>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Button
                          variant="outline"
                          size="icon"
                          onClick={onCopyCurl}
                          disabled={!selectedOperation.path}
                          aria-label="Copy cURL command"
                          className="border-primary-200 hover:bg-primary-50 dark:border-border dark:hover:bg-muted h-8 w-8"
                        >
                          <Terminal className="h-4 w-4" aria-hidden="true" />
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent className="max-w-xs">
                        <p className="font-medium">Copy cURL</p>
                        <p className="text-xs text-muted-foreground mt-1">
                          Full URL to the backend, same headers as Send (including Content-Type and X-Mock-Operation-Id).
                        </p>
                      </TooltipContent>
                    </Tooltip>
                  </TooltipProvider>
                  <TooltipProvider>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Button
                          onClick={onTest}
                          disabled={!selectedOperation.path || isTesting}
                          aria-label="Test endpoint"
                          className="bg-gradient-to-r from-primary-500 to-secondary-600 hover:from-primary-600 hover:to-secondary-700 text-white border-0 h-8 w-8 p-0"
                        >
                          {isTesting ? (
                            <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" aria-hidden="true" />
                          ) : (
                            <Play className="h-4 w-4" aria-hidden="true" />
                          )}
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent>Test Endpoint</TooltipContent>
                    </Tooltip>
                  </TooltipProvider>
                </div>
              </div>
            </div>

            {/* Row 2: Request Headers - Aligns with Response Headers */}
            <div className="flex-shrink-0">
              <Collapsible open={isRequestHeadersExpanded} onOpenChange={setIsRequestHeadersExpanded}>
                <div className="flex items-center justify-between mb-2 h-[20px]">
                  <CollapsibleTrigger asChild>
                    <Button variant="ghost" size="sm" className="p-0 h-auto hover:bg-transparent">
                      <div className="flex items-center space-x-2">
                        {isRequestHeadersExpanded ? (
                          <ChevronDown className="h-4 w-4 text-primary-700" />
                        ) : (
                          <ChevronRight className="h-4 w-4 text-primary-700" />
                        )}
                        <Label className="text-sm font-medium text-primary-700 cursor-pointer">
                          Request Headers
                          {headerRows.some(r => r.key.trim() || r.value.trim()) && (
                            <Badge variant="outline" className="ml-2 text-xs">
                              {headerRows.filter(r => r.key.trim() && r.value.trim()).length}
                            </Badge>
                          )}
                        </Label>
                      </div>
                    </Button>
                  </CollapsibleTrigger>
                </div>
                <CollapsibleContent className="space-y-3">
                  {isRequestHeadersExpanded && (
                    <Button variant="outline" size="sm" onClick={addHeaderRow} className="border-indigo-200 hover:bg-indigo-50 mb-2">
                      <Plus className="h-4 w-4 mr-1" />
                      Add Header
                    </Button>
                  )}

                  <div className="space-y-2">
                    {headerRows.map((row, index) => (
                      <div key={index} className="flex items-center space-x-2">
                        <Input
                          placeholder="Header Name"
                          value={row.key}
                          onChange={(e) => updateHeaderRow(index, 'key', e.target.value)}
                          className="font-mono text-sm border-primary-200 bg-primary-50/50"
                        />
                        <Input
                          placeholder="Header Value"
                          value={row.value}
                          onChange={(e) => updateHeaderRow(index, 'value', e.target.value)}
                          className="font-mono text-sm border-primary-200 bg-primary-50/50"
                        />
                        {headerRows.length > 1 && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => removeHeaderRow(index)}
                            className="border-red-200 hover:bg-red-50 text-red-600"
                          >
                            <X className="h-4 w-4" />
                          </Button>
                        )}
                      </div>
                    ))}
                  </div>
                </CollapsibleContent>
              </Collapsible>
            </div>

            {/* Row 3: Request Body (for POST/PUT/PATCH) - Aligns with Response Body */}
            <div className="flex-1 min-h-0 flex flex-col">
              {['POST', 'PUT', 'PATCH'].includes(selectedOperation.method || '') ? (
                <>
                  <div className="flex items-center justify-between mb-2 h-[28px]">
                    <Label className="text-sm font-medium text-primary-700">Request Body (JSON)</Label>
                    <div className="flex items-center space-x-2">
                      <Button variant="outline" size="sm" onClick={onBeautifyJson} className="border-primary-200 hover:bg-primary-50">
                        <Sparkles className="h-4 w-4 mr-1" />
                        Beautify
                      </Button>
                      <Button variant="outline" size="sm" onClick={onRegenerateRequest} className="border-primary-200 hover:bg-primary-50">
                        <Upload className="h-4 w-4 mr-1" />
                        Generate
                      </Button>
                    </div>
                  </div>
                  <Textarea
                    placeholder='{"name": "John Doe"}'
                    value={requestBody}
                    onChange={(e) => onRequestBodyChange(e.target.value)}
                    rows={8}
                    className="font-mono text-sm border-primary-200 bg-primary-50/50 resize-none flex-1 min-h-[200px]"
                  />
                </>
              ) : (
                <>
                  <div className="flex items-center justify-between mb-2 h-[28px] flex-shrink-0">
                    <span className="h-[20px]" aria-hidden="true" />
                  </div>
                  <div className="flex-1 min-h-[200px]" />
                </>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Right Panel - Response (theme tokens so dark mode matches Request card) */}
        <Card className="flex h-full flex-col border-primary-200 bg-gradient-to-br from-primary-50/30 to-secondary-50/30 dark:border-border dark:from-muted/40 dark:to-muted/20">
          <CardHeader className="h-[88px] flex-shrink-0 pb-3">
            <CardTitle className="flex h-[28px] items-center space-x-2 text-lg text-primary-700 dark:text-foreground">
              <Code className="h-5 w-5 shrink-0 text-primary-600 dark:text-muted-foreground" />
              <span>Response</span>
              {isTesting && (
                <Badge
                  variant="outline"
                  className="animate-pulse border-amber-500/40 bg-amber-500/10 text-amber-900 dark:text-amber-200"
                >
                  Testing...
                </Badge>
              )}
              {testResult && !isTesting && (
                <Badge
                  variant="outline"
                  className={
                    testResult.status >= 400
                      ? 'border-destructive/40 bg-destructive/10 text-destructive dark:text-red-300'
                      : 'border-emerald-500/40 bg-emerald-500/10 text-emerald-900 dark:text-emerald-200'
                  }
                >
                  {testResult.status} {testResult.statusText}
                </Badge>
              )}
            </CardTitle>
            <CardDescription className="h-[20px] text-primary-600 dark:text-muted-foreground">
              {testResult
                ? `Response received ${testResult.mockVariantId ? `(Mock Variant: ${testResult.mockVariantId})` : ''}`
                : 'Response will appear here after testing the endpoint'}
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-1 flex-col gap-4 p-6">
            {isTesting ? (
              <div className="col-span-full row-span-full flex flex-col items-center justify-center space-y-4 py-12">
                <div className="relative">
                  <div className="h-16 w-16 animate-spin rounded-full border-4 border-primary-200 border-t-primary-500 dark:border-muted dark:border-t-primary-400" />
                </div>
                <div className="text-center">
                  <p className="text-sm font-medium text-primary-700 dark:text-foreground">Sending request...</p>
                  <p className="mt-1 text-xs text-primary-600 dark:text-muted-foreground">Waiting for response</p>
                </div>
              </div>
            ) : testResult ? (
              <>
                <div className="flex-shrink-0">
                  <Label className="mb-2 flex h-[20px] items-center text-sm font-medium text-primary-700 dark:text-foreground">
                    Response Status
                  </Label>
                  <div className="rounded-lg border border-primary-200 bg-primary-50/50 p-3 dark:border-border dark:bg-muted/50">
                    <div className="flex flex-col space-y-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <Badge
                          variant="outline"
                          className={
                            testResult.status >= 400
                              ? 'border-destructive/40 bg-destructive/10 text-destructive dark:text-red-300'
                              : 'border-emerald-500/40 bg-emerald-500/10 text-emerald-900 dark:text-emerald-200'
                          }
                        >
                          {testResult.status} {testResult.statusText}
                        </Badge>
                        {testResult.mockVariantId && (
                          <Badge variant="outline" className="text-xs dark:border-border dark:text-muted-foreground">
                            Mock: {testResult.mockVariantId}
                          </Badge>
                        )}
                      </div>
                      {testResult.mockOperationId && (
                        <span className="text-xs text-primary-600 dark:text-muted-foreground">
                          Operation: {testResult.mockOperationId}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex-shrink-0">
                  <Collapsible open={isResponseHeadersExpanded} onOpenChange={setIsResponseHeadersExpanded}>
                    <div className="mb-2 flex h-[20px] items-center justify-between">
                      <CollapsibleTrigger asChild>
                        <Button variant="ghost" size="sm" className="h-auto p-0 hover:bg-transparent">
                          <div className="flex items-center space-x-2">
                            {isResponseHeadersExpanded ? (
                              <ChevronDown className="h-4 w-4 text-primary-700 dark:text-muted-foreground" />
                            ) : (
                              <ChevronRight className="h-4 w-4 text-primary-700 dark:text-muted-foreground" />
                            )}
                            <Label className="cursor-pointer text-sm font-medium text-primary-700 dark:text-foreground">
                              Response Headers
                              {testResult.headers && Object.keys(testResult.headers).length > 0 && (
                                <Badge variant="outline" className="ml-2 text-xs dark:border-border">
                                  {Object.keys(testResult.headers).length}
                                </Badge>
                              )}
                            </Label>
                          </div>
                        </Button>
                      </CollapsibleTrigger>
                    </div>
                    <CollapsibleContent className="space-y-2">
                      {testResult.headers && Object.keys(testResult.headers).length > 0 ? (
                        Object.entries(testResult.headers).map(([key, value]) => (
                          <div key={key} className="flex items-center space-x-2">
                            <Input
                              value={key}
                              readOnly
                              className="border-primary-200 bg-primary-50/50 font-mono text-sm dark:border-border dark:bg-muted/40"
                            />
                            <Input
                              value={typeof value === 'string' ? value : String(value)}
                              readOnly
                              className="border-primary-200 bg-primary-50/50 font-mono text-sm dark:border-border dark:bg-muted/40"
                            />
                          </div>
                        ))
                      ) : (
                        <div className="p-2 text-xs italic text-primary-600 dark:text-muted-foreground">
                          No response headers
                        </div>
                      )}
                    </CollapsibleContent>
                  </Collapsible>
                </div>

                <div className="flex min-h-0 flex-1 flex-col">
                  <div className="mb-2 flex h-[28px] flex-shrink-0 items-center justify-between">
                    <Label className="text-sm font-medium text-primary-700 dark:text-foreground">Response Body</Label>
                  </div>
                  <div className="relative min-h-0 flex-1">
                    <pre className="h-full max-h-[min(480px,50vh)] overflow-auto rounded-lg border border-primary-200 bg-primary-50/50 p-4 font-mono text-sm text-foreground shadow-sm dark:border-border dark:bg-muted/50 whitespace-pre-wrap break-words">
                      {typeof testResult.data === 'string'
                        ? testResult.data
                        : JSON.stringify(testResult.data, null, 2)}
                    </pre>
                  </div>
                </div>
              </>
            ) : (
              <div className="col-span-full row-span-full flex flex-col items-center justify-center space-y-4 py-12 text-center">
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary-100 dark:bg-muted">
                  <Code className="h-8 w-8 text-primary-600 opacity-50 dark:text-muted-foreground" />
                </div>
                <div>
                  <p className="mb-1 text-sm font-medium text-primary-700 dark:text-foreground">No response yet</p>
                  <p className="text-xs text-primary-600 dark:text-muted-foreground">
                    Use Test Endpoint to send a request and see the response here
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
