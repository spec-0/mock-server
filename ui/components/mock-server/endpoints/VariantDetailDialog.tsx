'use client';

import React from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Pencil, Copy } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { MockResponseVariant } from '@/services/mockServerAdminClient';

function formatJsonSafely(jsonString: string): string {
  try {
    const parsed = JSON.parse(jsonString);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return jsonString;
  }
}

function getHeadersAsObject(headers: unknown): Record<string, unknown> | null {
  if (!headers) return null;
  if (typeof headers === 'string') {
    try {
      return JSON.parse(headers) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
  if (typeof headers === 'object') return headers as Record<string, unknown>;
  return null;
}

function isCelVariant(v: MockResponseVariant): boolean {
  return !!(v.celExpression && String(v.celExpression).trim());
}

interface VariantDetailDialogProps {
  variant: MockResponseVariant | null;
  onClose: () => void;
  onEdit: (variant: MockResponseVariant) => void;
}

export default function VariantDetailDialog({ variant, onClose, onEdit }: VariantDetailDialogProps) {
  const { toast } = useToast();
  const open = variant !== null;
  const cel = variant && isCelVariant(variant);
  const headersObj = variant ? getHeadersAsObject(variant.headers) : null;
  const hasHeaders = headersObj !== null && Object.keys(headersObj).length > 0;

  const copy = async (label: string, text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      toast({ title: 'Copied', description: `${label} copied to clipboard.` });
    } catch {
      toast({ title: 'Copy failed', variant: 'destructive' });
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-3xl max-h-[90vh] flex flex-col gap-0 p-0">
        <DialogHeader className="px-6 pt-6 pb-4 shrink-0 border-b">
          <DialogTitle className="flex flex-wrap items-center gap-2 pr-8">
            <span>{variant?.responseName ?? 'Variant'}</span>
            {variant && (
              <>
                <Badge variant="secondary">{variant.statusCode}</Badge>
                {cel ? (
                  <Badge className="bg-purple-600 hover:bg-purple-600">CEL</Badge>
                ) : (
                  <Badge variant="outline">Static</Badge>
                )}
                {variant.isDefault && <Badge variant="default">Default</Badge>}
              </>
            )}
          </DialogTitle>
          <DialogDescription className="text-left">
            {variant?.operationId ? (
              <span className="font-mono text-xs">{variant.operationId}</span>
            ) : null}
            {variant && (
              <span className="block mt-1 text-muted-foreground">
                Display order {variant.displayOrder ?? 0}
                {variant.createdAt ? ` · Created ${new Date(variant.createdAt).toLocaleString()}` : ''}
              </span>
            )}
          </DialogDescription>
        </DialogHeader>

        <div className="overflow-y-auto max-h-[60vh] px-6">
          <div className="space-y-5 py-4">
            {cel && variant?.celExpression && (
              <div>
                <div className="flex items-center justify-between gap-2 mb-2">
                  <Label className="text-xs font-medium text-muted-foreground">CEL expression</Label>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs"
                    onClick={() => copy('CEL expression', variant.celExpression!)}
                  >
                    <Copy className="h-3 w-3 mr-1" />
                    Copy
                  </Button>
                </div>
                <div className="rounded-md border bg-muted/40 p-3">
                  <pre className="text-xs font-mono whitespace-pre-wrap break-words max-h-72 overflow-auto">
                    {variant.celExpression}
                  </pre>
                </div>
                <p className="text-xs text-muted-foreground mt-2">
                  Evaluated per request. Must return a map with{' '}
                  <code className="font-mono bg-muted px-1 rounded">status</code>,{' '}
                  <code className="font-mono bg-muted px-1 rounded">body</code>, optional{' '}
                  <code className="font-mono bg-muted px-1 rounded">headers</code>.
                </p>
              </div>
            )}

            <div>
              <div className="flex items-center justify-between gap-2 mb-2">
                <Label className="text-xs font-medium text-muted-foreground">
                  {cel ? 'Fallback response body (JSON)' : 'Response body (JSON)'}
                </Label>
                {(variant?.responseBody ?? '').trim() !== '' && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs"
                    onClick={() => variant && copy('Response body', variant.responseBody || '')}
                  >
                    <Copy className="h-3 w-3 mr-1" />
                    Copy
                  </Button>
                )}
              </div>
              <div className="rounded-md border bg-background p-3">
                {(variant?.responseBody ?? '').trim() === '' ? (
                  <p className="text-sm text-muted-foreground italic">
                    {cel
                      ? 'No fallback body stored. If CEL errors or returns null, the server may use an empty or default response.'
                      : 'Empty body'}
                  </p>
                ) : (
                  <pre className="text-xs font-mono whitespace-pre-wrap break-words">
                    {formatJsonSafely(variant!.responseBody || '')}
                  </pre>
                )}
              </div>
            </div>

            {hasHeaders && headersObj && (
              <div>
                <div className="flex items-center justify-between gap-2 mb-2">
                  <Label className="text-xs font-medium text-muted-foreground">Response headers (JSON)</Label>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs"
                    onClick={() => copy('Headers', JSON.stringify(headersObj, null, 2))}
                  >
                    <Copy className="h-3 w-3 mr-1" />
                    Copy
                  </Button>
                </div>
                <div className="rounded-md border bg-background p-3">
                  <pre className="text-xs font-mono whitespace-pre-wrap break-words">
                    {formatJsonSafely(JSON.stringify(headersObj, null, 2))}
                  </pre>
                </div>
              </div>
            )}
          </div>
        </div>

        <DialogFooter className="px-6 py-4 border-t shrink-0 gap-2 sm:gap-0">
          <Button type="button" variant="outline" onClick={onClose}>
            Done
          </Button>
          {variant && (
            <Button
              type="button"
              onClick={() => {
                onEdit(variant);
                onClose();
              }}
            >
              <Pencil className="h-4 w-4 mr-2" />
              Edit variant
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
