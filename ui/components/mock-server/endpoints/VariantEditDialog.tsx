'use client';

import React, { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { MockResponseVariant } from '@/services/mockServerAdminClient';

interface VariantEditDialogProps {
  variant: MockResponseVariant | null;
  onClose: () => void;
  availableStatusCodes: string[];
  onSubmit: (
    variantId: string,
    data: {
      responseName: string;
      statusCode: string;
      responseBody: string;
      headers: string;
      isDefault: boolean;
      displayOrder: number;
      celExpression?: string | null;
    }
  ) => Promise<void>;
}

function getHeadersAsObject(headers: any): Record<string, any> | null {
  if (!headers) return null;
  if (typeof headers === 'string') {
    try {
      return JSON.parse(headers);
    } catch {
      return null;
    }
  }
  if (typeof headers === 'object') return headers;
  return null;
}

export default function VariantEditDialog({
  variant,
  onClose,
  availableStatusCodes,
  onSubmit,
}: VariantEditDialogProps) {
  const [variantTab, setVariantTab] = useState<'static' | 'cel'>('static');
  const [responseName, setResponseName] = useState('');
  const [statusCode, setStatusCode] = useState('');
  const [displayOrder, setDisplayOrder] = useState(1);
  const [responseBody, setResponseBody] = useState('');
  const [headers, setHeaders] = useState('');
  const [isDefault, setIsDefault] = useState(false);
  const [celExpression, setCelExpression] = useState('');

  useEffect(() => {
    if (variant) {
      setResponseName(variant.responseName || '');
      setStatusCode(variant.statusCode || '');
      setDisplayOrder(variant.displayOrder || 1);
      setResponseBody(variant.responseBody || '');
      const headersObj = getHeadersAsObject(variant.headers);
      setHeaders(headersObj ? JSON.stringify(headersObj, null, 2) : '');
      setIsDefault(variant.isDefault || false);
      const cel = variant.celExpression ?? '';
      setCelExpression(cel);
      setVariantTab(cel ? 'cel' : 'static');
    }
  }, [variant]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!variant?.variantId) return;
    await onSubmit(variant.variantId, {
      responseName,
      statusCode,
      responseBody,
      headers,
      isDefault,
      displayOrder,
      celExpression: variantTab === 'cel' && celExpression.trim() ? celExpression.trim() : null,
    });
  };

  return (
    <Dialog open={variant !== null} onOpenChange={() => onClose()}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            Edit Response Variant
            <Badge variant="outline" className={variantTab === 'cel' ? 'text-purple-700 border-purple-300 bg-purple-50' : ''}>
              {variantTab === 'cel' ? 'CEL' : 'Static'}
            </Badge>
          </DialogTitle>
          <DialogDescription>Update the mock response variant</DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="edit-responseName">Response Name</Label>
              <Input
                id="edit-responseName"
                value={responseName}
                onChange={(e) => setResponseName(e.target.value)}
                required
              />
            </div>
            <div>
              <Label htmlFor="edit-statusCode">Status Code</Label>
              <Input
                id="edit-statusCode"
                value={statusCode}
                onChange={(e) => setStatusCode(e.target.value)}
                required
              />
            </div>
          </div>

          <div>
            <Label htmlFor="edit-displayOrder">Display Order</Label>
            <Input
              id="edit-displayOrder"
              type="number"
              value={displayOrder}
              onChange={(e) => setDisplayOrder(parseInt(e.target.value) || 1)}
            />
          </div>

          <Tabs value={variantTab} onValueChange={(v) => setVariantTab(v as 'static' | 'cel')}>
            <TabsList>
              <TabsTrigger value="static">Response Body</TabsTrigger>
              <TabsTrigger value="cel">CEL Expression</TabsTrigger>
            </TabsList>

            <TabsContent value="static" className="space-y-3 mt-3">
              <div>
                <Label htmlFor="edit-responseBody">Response Body (JSON)</Label>
                <Textarea
                  id="edit-responseBody"
                  value={responseBody}
                  onChange={(e) => setResponseBody(e.target.value)}
                  rows={6}
                  required={variantTab === 'static'}
                />
              </div>
              <div>
                <Label htmlFor="edit-headers">Headers (JSON)</Label>
                <Textarea
                  id="edit-headers"
                  value={headers}
                  onChange={(e) => setHeaders(e.target.value)}
                  rows={2}
                />
              </div>
            </TabsContent>

            <TabsContent value="cel" className="space-y-3 mt-3">
              <div className="rounded-md bg-purple-50 border border-purple-200 p-3 text-sm text-purple-800">
                CEL expressions are evaluated at request time. Available: <code className="font-mono text-xs">request.path_params</code>,{' '}
                <code className="font-mono text-xs">request.query_params</code>,{' '}
                <code className="font-mono text-xs">request.headers</code>,{' '}
                <code className="font-mono text-xs">request.body</code>,{' '}
                <code className="font-mono text-xs">env.*</code>,{' '}
                <code className="font-mono text-xs">uuid()</code>,{' '}
                <code className="font-mono text-xs">now()</code>,{' '}
                <code className="font-mono text-xs">randomInt(min, max)</code>
              </div>
              <div>
                <Label htmlFor="edit-celExpression">CEL Expression</Label>
                <Textarea
                  id="edit-celExpression"
                  value={celExpression}
                  onChange={(e) => setCelExpression(e.target.value)}
                  rows={10}
                  required={variantTab === 'cel'}
                  className="font-mono text-sm"
                  placeholder={'request.path_params.id.startsWith("99")\n  ? {"status": 404, "body": {"error": "not found"}}\n  : {"status": 200, "body": {"id": request.path_params.id}}'}
                />
              </div>
              <div>
                <Label htmlFor="edit-headers-cel">Response headers (JSON)</Label>
                <Textarea
                  id="edit-headers-cel"
                  value={headers}
                  onChange={(e) => setHeaders(e.target.value)}
                  rows={2}
                  placeholder='{"X-Custom": "value"}'
                />
              </div>
              <div>
                <Label htmlFor="edit-responseBodyFallback">Fallback Response Body (JSON)</Label>
                <Textarea
                  id="edit-responseBodyFallback"
                  value={responseBody}
                  onChange={(e) => setResponseBody(e.target.value)}
                  rows={3}
                  placeholder='{"message": "fallback if CEL fails"}'
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
              id="edit-isDefault"
              checked={isDefault}
              onChange={(e) => setIsDefault(e.target.checked)}
              className="rounded"
            />
            <Label htmlFor="edit-isDefault">Set as default response</Label>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit">Update Variant</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
