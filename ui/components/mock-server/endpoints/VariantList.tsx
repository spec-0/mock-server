'use client';

import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';
import { Plus, Edit, Trash2, Copy, ChevronDown, ChevronRight, Eye, Code, Settings, Sparkles } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { MockResponseVariant, MockServer } from '@/services/mockServerAdminClient';

function isCelVariant(v: MockResponseVariant): boolean {
  return !!(v.celExpression && String(v.celExpression).trim());
}

interface VariantListProps {
  operationId: string;
  variants: MockResponseVariant[];
  expandedVariants: Set<string>;
  onToggleExpand: (variantId: string) => void;
  onViewDetail: (variant: MockResponseVariant) => void;
  onEdit: (variant: MockResponseVariant) => void;
  onDelete: (variantId: string) => Promise<void>;
  onCreateClick: () => void;
  mockServer: MockServer;
  selectedOperationPath?: string;
}

function formatJsonSafely(jsonString: string): string {
  try {
    const parsed = JSON.parse(jsonString);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return jsonString;
  }
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
  if (typeof headers === 'object') {
    return headers;
  }
  return null;
}

function hasHeadersValue(headers: any): boolean {
  const headersObj = getHeadersAsObject(headers);
  return headersObj !== null && Object.keys(headersObj).length > 0;
}

export default function VariantList({
  operationId,
  variants,
  expandedVariants,
  onToggleExpand,
  onViewDetail,
  onEdit,
  onDelete,
  onCreateClick,
  mockServer,
  selectedOperationPath,
}: VariantListProps) {
  const { toast } = useToast();

  const sortedVariants = [...variants].sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));

  return (
    <Card className="border-indigo-200 bg-gradient-to-br from-indigo-50/30 to-purple-50/30">
      <CardHeader className="bg-gradient-to-r from-indigo-500 to-purple-600 text-white rounded-t-lg">
        <CardTitle className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Settings className="h-5 w-5" />
            <span>Response Variants</span>
          </div>
          <Button
            className="bg-white/20 hover:bg-white/30 text-white border-white/30"
            onClick={onCreateClick}
          >
            <Plus className="h-4 w-4 mr-2" />
            Add Variant
          </Button>
        </CardTitle>
      </CardHeader>
      <CardContent>
        {sortedVariants.length > 0 ? (
          <div className="space-y-3">
            {sortedVariants.map((variant) => {
              const isExpanded = variant.variantId ? expandedVariants.has(variant.variantId) : false;
              const hasHeaders = hasHeadersValue(variant.headers);
              const cel = isCelVariant(variant);

              return (
                <Collapsible
                  key={variant.variantId}
                  open={isExpanded}
                  onOpenChange={() => variant.variantId && onToggleExpand(variant.variantId)}
                >
                  <div className="border rounded-lg bg-card hover:bg-muted/50 transition-colors">
                    <CollapsibleTrigger asChild>
                      <div className="flex items-center justify-between p-4 cursor-pointer">
                        <div className="flex items-center space-x-3">
                          <div className="flex items-center space-x-2">
                            {isExpanded ? (
                              <ChevronDown className="h-4 w-4 text-muted-foreground" />
                            ) : (
                              <ChevronRight className="h-4 w-4 text-muted-foreground" />
                            )}
                            <h4 className="font-medium text-sm">{variant.responseName}</h4>
                          </div>
                          <div className="flex items-center space-x-2">
                            <Badge
                              variant={variant.isDefault ? 'default' : 'secondary'}
                              className="text-xs"
                            >
                              {variant.statusCode}
                            </Badge>
                            {variant.isDefault && (
                              <Badge variant="outline" className="text-xs">
                                Default
                              </Badge>
                            )}
                            {cel && (
                              <Badge className="text-xs bg-purple-600 hover:bg-purple-600">
                                <Sparkles className="h-3 w-3 mr-1" />
                                CEL
                              </Badge>
                            )}
                            {hasHeaders && (
                              <Badge variant="outline" className="text-xs">
                                <Eye className="h-3 w-3 mr-1" />
                                Headers
                              </Badge>
                            )}
                          </div>
                        </div>
                        <div className="flex items-center space-x-1">
                          <Button
                            variant="ghost"
                            size="sm"
                            title="View details"
                            onClick={(e) => {
                              e.stopPropagation();
                              onViewDetail(variant);
                            }}
                            className="h-8 w-8 p-0"
                          >
                            <Eye className="h-3 w-3" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            title="Edit variant"
                            onClick={(e) => {
                              e.stopPropagation();
                              onEdit(variant);
                            }}
                            className="h-8 w-8 p-0"
                          >
                            <Edit className="h-3 w-3" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={(e) => {
                              e.stopPropagation();
                              variant.variantId && onDelete(variant.variantId);
                            }}
                            className="h-8 w-8 p-0 text-destructive hover:text-destructive"
                          >
                            <Trash2 className="h-3 w-3" />
                          </Button>
                        </div>
                      </div>
                    </CollapsibleTrigger>

                    <CollapsibleContent>
                      <div className="px-4 pb-4 space-y-4 border-t bg-muted/20">
                        {cel && variant.celExpression && (
                          <div>
                            <Label className="text-xs font-medium text-muted-foreground mb-2 block">
                              CEL expression
                            </Label>
                            <div className="bg-purple-950/5 border border-purple-200 rounded-md p-3">
                              <pre className="text-xs font-mono whitespace-pre-wrap break-words overflow-auto max-h-48">
                                {variant.celExpression}
                              </pre>
                            </div>
                          </div>
                        )}

                        <div>
                          <Label className="text-xs font-medium text-muted-foreground mb-2 block">
                            {cel ? 'Fallback response body (JSON)' : 'Response body (JSON)'}
                          </Label>
                          <div className="bg-background border rounded-md p-3">
                            {(variant.responseBody || '').trim() === '' ? (
                              <p className="text-xs text-muted-foreground italic">
                                {cel
                                  ? 'No fallback JSON stored (used if CEL fails or returns null).'
                                  : 'Empty'}
                              </p>
                            ) : (
                              <pre className="text-xs font-mono whitespace-pre-wrap break-words overflow-auto max-h-64">
                                {formatJsonSafely(variant.responseBody || '')}
                              </pre>
                            )}
                          </div>
                        </div>

                        {/* Headers */}
                        {hasHeaders && (() => {
                          const headersObj = getHeadersAsObject(variant.headers);
                          return headersObj ? (
                            <div>
                              <Label className="text-xs font-medium text-muted-foreground mb-2 block">
                                Response Headers
                              </Label>
                              <div className="bg-background border rounded-md p-3">
                                <pre className="text-xs font-mono whitespace-pre-wrap break-words">
                                  {formatJsonSafely(JSON.stringify(headersObj, null, 2))}
                                </pre>
                              </div>
                            </div>
                          ) : null;
                        })()}

                        {/* Metadata */}
                        <div className="flex items-center justify-between text-xs text-muted-foreground pt-2 border-t">
                          <div className="flex items-center space-x-4">
                            <span>Order: {variant.displayOrder || 0}</span>
                            {variant.createdAt && (
                              <span>Created: {new Date(variant.createdAt).toLocaleString()}</span>
                            )}
                          </div>
                          <div className="flex items-center space-x-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => {
                                const url = `${process.env.NEXT_PUBLIC_API_BASE_URL}/v2/mock-server/${mockServer.mockServerId}${selectedOperationPath || ''}`;
                                navigator.clipboard.writeText(url);
                                toast({
                                  title: 'URL Copied',
                                  description: 'Mock endpoint URL copied to clipboard.',
                                });
                              }}
                              className="h-6 px-2 text-xs"
                            >
                              <Copy className="h-3 w-3 mr-1" />
                              Copy URL
                            </Button>
                          </div>
                        </div>
                      </div>
                    </CollapsibleContent>
                  </div>
                </Collapsible>
              );
            })}
          </div>
        ) : (
          <div className="text-center py-12 text-muted-foreground">
            <Code className="h-12 w-12 mx-auto mb-4 opacity-50" />
            <h3 className="text-lg font-semibold mb-2">No Response Variants</h3>
            <p className="text-sm mb-4">Create response variants to customize mock responses for this operation.</p>
            <Button onClick={onCreateClick}>
              <Plus className="h-4 w-4 mr-2" />
              Create First Variant
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
