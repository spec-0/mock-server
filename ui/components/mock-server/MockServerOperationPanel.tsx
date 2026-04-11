'use client';

import React, { useState, useMemo } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Switch } from '@/components/ui/switch';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Search, ChevronRight, ChevronDown, Play, Pause, Settings, PanelLeftClose } from 'lucide-react';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useToast } from '@/hooks/use-toast';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { MockOperationConfig } from '@/services/mockServerAdminClient';
import type { Operation } from '@/lib/api/types';

interface MockServerOperationPanelProps {
  operations: Operation[];
  operationConfigs: MockOperationConfig[];
  selectedOperationId?: string;
  onOperationSelect: (operationId: string) => void;
  onOperationToggle: (operationId: string, enabled: boolean) => Promise<void>;
  onStrategyChange?: (operationId: string, strategy: string) => Promise<void>;
  variantsCount: Record<string, number>;
  onCollapseSidebar?: () => void;
}

interface GroupedOperations {
  [path: string]: {
    operations: Operation[];
    enabledCount: number;
    totalCount: number;
  };
}

export default function MockServerOperationPanel({
  operations,
  operationConfigs,
  selectedOperationId,
  onOperationSelect,
  onOperationToggle,
  onStrategyChange,
  variantsCount,
  onCollapseSidebar,
}: MockServerOperationPanelProps) {
  const { toast } = useToast();
  const [searchTerm, setSearchTerm] = useState('');
  const [expandedPaths, setExpandedPaths] = useState<Set<string>>(new Set());

  const configMap = useMemo(() => {
    const map = new Map<string, MockOperationConfig>();
    operationConfigs.forEach((config) => {
      map.set(config.operationId, config);
    });
    return map;
  }, [operationConfigs]);

  const groupedOperations = useMemo((): GroupedOperations => {
    const grouped: GroupedOperations = {};

    operations.forEach((operation) => {
      const path = operation.path || '/';
      if (!grouped[path]) {
        grouped[path] = {
          operations: [],
          enabledCount: 0,
          totalCount: 0,
        };
      }

      grouped[path].operations.push(operation);
      grouped[path].totalCount++;

      const config = configMap.get(operation.operationId || '');
      if (config?.isEnabled) {
        grouped[path].enabledCount++;
      }
    });

    Object.values(grouped).forEach((group) => {
      group.operations.sort((a, b) => {
        const methodOrder = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];
        const aIndex = methodOrder.indexOf(a.method || '');
        const bIndex = methodOrder.indexOf(b.method || '');
        return aIndex - bIndex;
      });
    });

    return grouped;
  }, [operations, configMap]);

  const filteredGroupedOperations = useMemo(() => {
    if (!searchTerm.trim()) {
      return groupedOperations;
    }

    const filtered: GroupedOperations = {};
    const searchLower = searchTerm.toLowerCase();

    Object.entries(groupedOperations).forEach(([path, group]) => {
      const matchingOperations = group.operations.filter((operation) => {
        const operationId = operation.operationId || '';
        const method = operation.method || '';
        const pathStr = operation.path || '';

        return (
          operationId.toLowerCase().includes(searchLower) ||
          method.toLowerCase().includes(searchLower) ||
          pathStr.toLowerCase().includes(searchLower)
        );
      });

      if (matchingOperations.length > 0) {
        filtered[path] = {
          operations: matchingOperations,
          enabledCount: matchingOperations.filter((op) => {
            const config = configMap.get(op.operationId || '');
            return config?.isEnabled;
          }).length,
          totalCount: matchingOperations.length,
        };
      }
    });

    return filtered;
  }, [groupedOperations, searchTerm, configMap]);

  const handleOperationToggle = async (operationId: string, enabled: boolean) => {
    try {
      await onOperationToggle(operationId, enabled);
      toast({
        title: enabled ? 'Operation Enabled' : 'Operation Disabled',
        description: `Operation has been ${enabled ? 'enabled' : 'disabled'} successfully.`,
      });
    } catch {
      toast({
        title: 'Error',
        description: 'Failed to update operation status.',
        variant: 'destructive',
      });
    }
  };

  const togglePathExpansion = (path: string) => {
    const newExpanded = new Set(expandedPaths);
    if (newExpanded.has(path)) {
      newExpanded.delete(path);
    } else {
      newExpanded.add(path);
    }
    setExpandedPaths(newExpanded);
  };

  const getMethodColor = (method: string) => {
    switch (method.toUpperCase()) {
      case 'GET':
        return 'bg-green-100 text-green-800';
      case 'POST':
        return 'bg-primary-100 text-primary-800';
      case 'PUT':
        return 'bg-yellow-100 text-yellow-800';
      case 'PATCH':
        return 'bg-orange-100 text-orange-800';
      case 'DELETE':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <Card className="flex h-full min-h-0 flex-col border-primary-200 bg-gradient-to-br from-primary-50/30 to-secondary-50/30">
      <CardHeader className="shrink-0 pb-3 bg-gradient-to-r from-primary-500 to-secondary-600 text-white rounded-t-lg">
        <CardTitle className="flex items-center justify-between gap-2">
          <div className="flex min-w-0 items-center space-x-2">
            <Settings className="h-5 w-5 shrink-0" aria-hidden />
            <span className="truncate">Operations</span>
          </div>
          {onCollapseSidebar && (
            <TooltipProvider delayDuration={300}>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={onCollapseSidebar}
                    className="h-9 w-9 shrink-0 text-white hover:bg-white/20"
                    aria-expanded={true}
                    aria-controls="operations-sidebar-panel"
                    aria-label="Collapse operations sidebar"
                  >
                    <PanelLeftClose className="h-4 w-4" />
                  </Button>
                </TooltipTrigger>
                <TooltipContent side="bottom" align="end" className="max-w-[220px]">
                  <p className="font-medium">Collapse sidebar</p>
                  <p className="text-xs text-muted-foreground mt-0.5">More room for testing and variants. Use the strip on the left to bring it back.</p>
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>
          )}
        </CardTitle>
        <CardDescription className="text-primary-100">
          Manage mock responses for each API operation
        </CardDescription>

        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-primary-300" />
          <Input
            placeholder="Search operations..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10 border-primary-200 bg-white/90 text-gray-900 placeholder-primary-400"
          />
        </div>
      </CardHeader>

      <CardContent className="min-h-0 flex-1 overflow-hidden p-0">
        <ScrollArea className="h-[calc(100vh-260px)] min-h-[200px]">
          <div className="p-4 space-y-2">
            {Object.keys(filteredGroupedOperations).length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                <p>No operations found</p>
                {searchTerm && <p className="text-sm mt-1">Try adjusting your search terms</p>}
              </div>
            ) : (
              Object.entries(filteredGroupedOperations).map(([path, group]) => {
                const isExpanded = expandedPaths.has(path);
                const isAllEnabled = group.enabledCount === group.totalCount;
                const isAllDisabled = group.enabledCount === 0;

                return (
                  <div key={path} className="border rounded-lg">
                    <div
                      className="flex items-center justify-between p-3 cursor-pointer hover:bg-muted/50"
                      onClick={() => togglePathExpansion(path)}
                    >
                      <div className="flex items-center space-x-2">
                        {isExpanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                        <code className="text-sm font-mono">{path}</code>
                        <Badge variant="outline" className="text-xs">
                          {group.enabledCount}/{group.totalCount}
                        </Badge>
                      </div>

                      <div className="flex items-center space-x-2">
                        <Switch
                          checked={isAllEnabled}
                          onClick={(e) => e.stopPropagation()}
                          onCheckedChange={(checked) => {
                            group.operations.forEach((operation) => {
                              if (operation.operationId) {
                                handleOperationToggle(operation.operationId, checked);
                              }
                            });
                          }}
                        />
                        {isAllEnabled ? (
                          <Play className="h-4 w-4 text-green-600" />
                        ) : isAllDisabled ? (
                          <Pause className="h-4 w-4 text-red-600" />
                        ) : (
                          <Settings className="h-4 w-4 text-yellow-600" />
                        )}
                      </div>
                    </div>

                    {isExpanded && (
                      <div className="border-t bg-muted/20">
                        {group.operations.map((operation) => {
                          const config = configMap.get(operation.operationId || '');
                          const isSelected = selectedOperationId === operation.operationId;
                          const variantCount = variantsCount[operation.operationId || ''] || 0;

                          return (
                            <div
                              key={operation.operationId}
                              className={`flex items-center justify-between p-3 cursor-pointer hover:bg-muted/50 ${
                                isSelected ? 'bg-primary/10 border-l-2 border-primary' : ''
                              }`}
                              onClick={() => onOperationSelect(operation.operationId || '')}
                            >
                              <div className="flex items-center space-x-3">
                                <Badge className={`text-xs ${getMethodColor(operation.method || '')}`}>
                                  {operation.method}
                                </Badge>
                                <div className="flex flex-col">
                                  <span className="text-sm font-medium">{operation.operationId}</span>
                                  {variantCount > 0 && (
                                    <span className="text-xs text-muted-foreground">
                                      {variantCount} variant{variantCount !== 1 ? 's' : ''}
                                    </span>
                                  )}
                                </div>
                              </div>

                              <div className="flex items-center space-x-2" onClick={(e) => e.stopPropagation()}>
                                {onStrategyChange && (
                                  <Select
                                    value={config?.responseStrategy || 'SERVER_DEFAULT'}
                                    onValueChange={(value) => {
                                      if (operation.operationId) {
                                        onStrategyChange(
                                          operation.operationId,
                                          value === 'SERVER_DEFAULT' ? '' : value,
                                        );
                                      }
                                    }}
                                  >
                                    <SelectTrigger className="h-7 w-36 text-xs">
                                      <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                      <SelectItem value="SERVER_DEFAULT">Server default</SelectItem>
                                      <SelectItem value="RANDOM">Random</SelectItem>
                                      <SelectItem value="ROUND_ROBIN">Round Robin</SelectItem>
                                      <SelectItem value="SEQUENTIAL">Sequential</SelectItem>
                                      <SelectItem value="DEFAULT_ONLY">Default Only</SelectItem>
                                    </SelectContent>
                                  </Select>
                                )}
                                <Switch
                                  checked={config?.isEnabled ?? true}
                                  onCheckedChange={(checked) => {
                                    if (operation.operationId) {
                                      handleOperationToggle(operation.operationId, checked);
                                    }
                                  }}
                                />
                                {config?.isEnabled ? (
                                  <Play className="h-4 w-4 text-green-600" />
                                ) : (
                                  <Pause className="h-4 w-4 text-red-600" />
                                )}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </ScrollArea>
      </CardContent>
    </Card>
  );
}
