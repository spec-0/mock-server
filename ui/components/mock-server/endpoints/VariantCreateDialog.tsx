'use client';

import React, { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';

const CEL_PLACEHOLDER = `// CEL expression — evaluated at request time.
// Must return a map: {"status": int, "body": any, "headers": map (optional)}
//
// Available context:
//   request.method, request.path
//   request.path_params.id   (from path template e.g. /users/{id})
//   request.query_params.filter
//   request.headers["authorization"]
//   request.body             (parsed JSON or null)
//   env.MY_KEY               (per-server env vars)
//
// Built-in functions: uuid(), now(), randomInt(min, max)
//
// Example:
request.path_params.id.startsWith("99")
  ? {"status": 404, "body": {"error": "not found"}}
  : {"status": 200, "body": {"id": request.path_params.id, "name": "Jane Doe"}}`;

interface VariantCreateDialogProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  operationId: string;
  availableStatusCodes: string[];
  onSubmit: (data: {
    operationId: string;
    responseName: string;
    statusCode: string;
    responseBody: string;
    headers: string;
    isDefault: boolean;
    displayOrder: number;
    celExpression?: string | null;
  }) => Promise<void>;
}

export default function VariantCreateDialog({
  isOpen,
  onOpenChange,
  operationId,
  availableStatusCodes,
  onSubmit,
}: VariantCreateDialogProps) {
  const [variantTab, setVariantTab] = useState<'static' | 'cel'>('static');
  const [responseName, setResponseName] = useState('');
  const [statusCode, setStatusCode] = useState('');
  const [displayOrder, setDisplayOrder] = useState(1);
  const [responseBody, setResponseBody] = useState('');
  const [headers, setHeaders] = useState('');
  const [isDefault, setIsDefault] = useState(false);
  const [celExpression, setCelExpression] = useState('');

  const resetForm = () => {
    setVariantTab('static');
    setResponseName('');
    setStatusCode('');
    setDisplayOrder(1);
    setResponseBody('');
    setHeaders('');
    setIsDefault(false);
    setCelExpression('');
  };

  const handleOpenChange = (open: boolean) => {
    if (!open) resetForm();
    onOpenChange(open);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSubmit({
      operationId,
      responseName,
      statusCode,
      responseBody,
      headers,
      isDefault,
      displayOrder,
      celExpression: variantTab === 'cel' && celExpression.trim() ? celExpression.trim() : null,
    });
    resetForm();
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            Create Response Variant
            <Badge aria-hidden="true" variant="outline" className={variantTab === 'cel' ? 'text-purple-700 border-purple-300 bg-purple-50' : ''}>
              {variantTab === 'cel' ? 'CEL' : 'Static'}
            </Badge>
          </DialogTitle>
          <DialogDescription>
            Create a new mock response variant for {operationId}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="responseName">Response Name</Label>
              <Input
                id="responseName"
                placeholder="Success Response"
                value={responseName}
                onChange={(e) => setResponseName(e.target.value)}
                required
              />
            </div>
            <div>
              <Label htmlFor="statusCode">Status Code</Label>
              <Select value={statusCode} onValueChange={setStatusCode} required>
                <SelectTrigger>
                  <SelectValue placeholder="Select status code..." />
                </SelectTrigger>
                <SelectContent>
                  {availableStatusCodes.map((code) => (
                    <SelectItem key={code} value={code}>{code}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div>
            <Label htmlFor="displayOrder">Display Order</Label>
            <Input
              id="displayOrder"
              type="number"
              min={0}
              max={9999}
              value={displayOrder}
              onChange={(e) => setDisplayOrder(parseInt(e.target.value) || 1)}
            />
          </div>

          {/* Response body / CEL tabs */}
          <Tabs value={variantTab} onValueChange={(v) => setVariantTab(v as 'static' | 'cel')}>
            <TabsList>
              <TabsTrigger value="static">Response Body</TabsTrigger>
              <TabsTrigger value="cel">CEL Expression</TabsTrigger>
            </TabsList>

            <TabsContent value="static" className="space-y-3 mt-3">
              <div>
                <Label htmlFor="responseBody">Response Body (JSON)</Label>
                <Textarea
                  id="responseBody"
                  placeholder='{"message": "success"}'
                  rows={6}
                  value={responseBody}
                  onChange={(e) => setResponseBody(e.target.value)}
                  required={variantTab === 'static'}
                />
              </div>
              <div>
                <Label htmlFor="headers">Headers (JSON)</Label>
                <Textarea
                  id="headers"
                  placeholder='{"Content-Type": "application/json"}'
                  rows={2}
                  value={headers}
                  onChange={(e) => setHeaders(e.target.value)}
                />
              </div>
            </TabsContent>

            <TabsContent value="cel" className="space-y-3 mt-3">
              <div className="rounded-md bg-purple-50 border border-purple-200 p-3 text-sm text-purple-800">
                CEL expressions are evaluated at request time using the incoming request context.
                Use the MCP tool <code className="font-mono text-xs bg-purple-100 px-1 rounded">create_cel_variant</code> in
                Claude Code or Cursor to generate expressions from a natural-language description.
              </div>
              <div>
                <Label htmlFor="celExpression">CEL Expression</Label>
                <Textarea
                  id="celExpression"
                  placeholder={CEL_PLACEHOLDER}
                  rows={10}
                  value={celExpression}
                  onChange={(e) => setCelExpression(e.target.value)}
                  required={variantTab === 'cel'}
                  className="font-mono text-sm"
                />
              </div>
              <div>
                <Label htmlFor="headers-cel">Response headers (JSON)</Label>
                <Textarea
                  id="headers-cel"
                  placeholder='{"X-Custom": "value"}'
                  rows={2}
                  value={headers}
                  onChange={(e) => setHeaders(e.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="responseBodyFallback">Fallback Response Body (JSON)</Label>
                <Textarea
                  id="responseBodyFallback"
                  placeholder='{"message": "fallback if CEL fails"}'
                  rows={3}
                  value={responseBody}
                  onChange={(e) => setResponseBody(e.target.value)}
                />
                <p className="text-xs text-muted-foreground mt-1">
                  Used if the CEL expression throws or returns null.
                </p>
              </div>
            </TabsContent>
          </Tabs>

          <div className="flex items-center space-x-2">
            <input
              type="checkbox"
              id="isDefault"
              checked={isDefault}
              onChange={(e) => setIsDefault(e.target.checked)}
              className="rounded"
            />
            <Label htmlFor="isDefault">Set as default response</Label>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit">Create Variant</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
