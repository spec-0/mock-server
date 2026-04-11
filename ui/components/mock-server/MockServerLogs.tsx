'use client';

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { RefreshCw, Clock, CheckCircle, XCircle, ChevronDown, ChevronRight, Loader2 } from 'lucide-react';
import { MockRequestLog } from '@/services/mockServerAdminClient';
import { JsonViewer } from '@/components/JsonViewer';

interface LogsClient {
  getLogs(mockServerId: string, limit?: number, offset?: number): Promise<MockRequestLog[]>;
}

interface MockServerLogsProps {
  mockServerId: string;
  apiClient: LogsClient;
  onRefresh?: () => void;
}

const PAGE_SIZE = 20;

export default function MockServerLogs({ mockServerId, apiClient, onRefresh }: MockServerLogsProps) {
  const [logs, setLogs] = useState<MockRequestLog[]>([]);
  const [expandedLogIds, setExpandedLogIds] = useState<Set<string>>(new Set());
  const [hasMore, setHasMore] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [offset, setOffset] = useState(0);
  const [error, setError] = useState<string | null>(null);
  
  const sentinelRef = useRef<HTMLDivElement>(null);

  const loadLogs = useCallback(async (currentOffset: number, append = false) => {
    try {
      if (append) {
        setIsLoadingMore(true);
      } else {
        setIsRefreshing(true);
      }
      
      if (!mockServerId) {
        throw new Error('Mock server ID is required');
      }
      
      // Backend returns logs sorted by timestamp DESC (newest first)
      // Use offset-based pagination to get the latest logs first
      const newLogs = await apiClient.getLogs(mockServerId, PAGE_SIZE, currentOffset);
      
      // Backend already returns logs sorted DESC, but sort again to ensure consistency
      const sortedLogs = [...newLogs].sort((a, b) => {
        const dateA = new Date(a.createdAt || 0).getTime();
        const dateB = new Date(b.createdAt || 0).getTime();
        return dateB - dateA; // Descending order (newest first)
      });

      if (append) {
        // When appending, merge with existing logs (keep newest first)
        // Since backend returns newest first, new logs should be appended to the end
        // Use functional update to avoid needing logs in dependency array
        setLogs(prevLogs => [...prevLogs, ...sortedLogs]);
      } else {
        // Initial load: just set the logs (they're already newest first)
        setLogs(sortedLogs);
        setExpandedLogIds(new Set());
      }
      
      // If we got less than PAGE_SIZE logs, there are no more to load
      setHasMore(newLogs.length === PAGE_SIZE);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load logs');
      console.error('Error loading logs:', err);
    } finally {
      setIsLoadingMore(false);
      setIsRefreshing(false);
    }
  }, [apiClient, mockServerId]);

  const loadMore = useCallback(() => {
    if (!hasMore || isLoadingMore) return;
    
    const newOffset = offset + PAGE_SIZE;
    setOffset(newOffset);
    loadLogs(newOffset, true);
  }, [hasMore, isLoadingMore, offset, loadLogs]);

  const refresh = useCallback(() => {
    setOffset(0);
    setHasMore(true);
    loadLogs(0, false);
    onRefresh?.();
  }, [loadLogs, onRefresh]);

  const toggleExpanded = (logId: string) => {
    setExpandedLogIds(prev => {
      const newSet = new Set(prev);
      if (newSet.has(logId)) {
        newSet.delete(logId);
      } else {
        newSet.add(logId);
      }
      return newSet;
    });
  };

  const getStatusIcon = (statusCode: string) => {
    const code = parseInt(statusCode);
    if (code >= 200 && code < 300) {
      return <CheckCircle className="h-4 w-4 text-green-500" />;
    } else if (code >= 400) {
      return <XCircle className="h-4 w-4 text-red-500" />;
    }
    return <Clock className="h-4 w-4 text-yellow-500" />;
  };

  const getStatusColor = (statusCode: string) => {
    const code = parseInt(statusCode);
    if (code >= 200 && code < 300) return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200';
    if (code >= 400) return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200';
    return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200';
  };

  const getMethodColor = (method: string) => {
    switch (method.toUpperCase()) {
      case 'GET': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200';
      case 'POST': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200';
      case 'PUT': return 'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-200';
      case 'DELETE': return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200';
      case 'PATCH': return 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200';
      default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200';
    }
  };

  // Intersection Observer for infinite scroll
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !isLoadingMore) {
          loadMore();
        }
      },
      { threshold: 0.1 }
    );

    if (sentinelRef.current) {
      observer.observe(sentinelRef.current);
    }

    return () => observer.disconnect();
  }, [hasMore, isLoadingMore, loadMore]);

  // Initial load
  useEffect(() => {
    if (mockServerId) {
      loadLogs(0, false);
    }
  }, [loadLogs, mockServerId]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold">Request Logs</h2>
          <p className="text-muted-foreground">Recent requests and responses to your mock server</p>
        </div>
        <Button 
          variant="outline" 
          onClick={refresh}
          disabled={isRefreshing}
        >
          <RefreshCw className={`h-4 w-4 mr-2 ${isRefreshing ? 'animate-spin' : ''}`} />
          Refresh
        </Button>
      </div>

      {error && (
        <Card className="border-red-200 bg-red-50 dark:border-red-800 dark:bg-red-900/20">
          <CardContent className="p-4">
            <p className="text-red-800 dark:text-red-200">{error}</p>
            <Button 
              variant="outline" 
              size="sm" 
              onClick={refresh}
              className="mt-2"
            >
              Try Again
            </Button>
          </CardContent>
        </Card>
      )}

      {logs.length === 0 && !error && !isRefreshing ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Clock className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold mb-2">No Logs Yet</h3>
            <p className="text-muted-foreground text-center">
              Request logs will appear here once your mock server starts receiving requests.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {logs.map((log) => {
            const logId = log.logId || `log-${Math.random()}`;
            const isExpanded = expandedLogIds.has(logId);
            
            return (
              <Card key={logId} className="transition-all duration-200 hover:shadow-md">
                <CardContent className="p-0">
                  {/* Collapsed Row */}
                  <div 
                    className="p-4 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                    onClick={() => toggleExpanded(logId)}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-3 flex-1 min-w-0">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-6 w-6 p-0 flex-shrink-0"
                        >
                          {isExpanded ? (
                            <ChevronDown className="h-4 w-4" />
                          ) : (
                            <ChevronRight className="h-4 w-4" />
                          )}
                        </Button>
                        
                        {getStatusIcon(log.responseStatusCode || '200')}
                        
                        <div className="flex items-center space-x-2 min-w-0 flex-1">
                          <Badge 
                            variant="outline" 
                            className={`${getMethodColor(log.httpMethod || 'GET')} font-mono text-xs`}
                          >
                            {log.httpMethod || 'GET'}
                          </Badge>
                          <span className="font-mono text-sm truncate">
                            {log.requestPath || '/'}
                          </span>
                        </div>
                      </div>
                      
                      <div className="flex items-center space-x-3 flex-shrink-0">
                        <Badge className={getStatusColor(log.responseStatusCode || '200')}>
                          {log.responseStatusCode || '200'}
                        </Badge>
                        <span className="text-sm text-muted-foreground font-mono">
                          {new Date(log.createdAt || Date.now()).toLocaleTimeString()}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Expanded Content - Separate Request & Response */}
                  {isExpanded && (
                    <div className="border-t bg-gray-50 dark:bg-gray-800/50 p-4">
                      <div className="space-y-6">
                        {/* Request Section */}
                        <div className="space-y-3">
                          <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                            <span className="w-2 h-2 bg-blue-500 rounded-full"></span>
                            Request
                          </h4>
                          <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
                            {/* Request Headers */}
                            <JsonViewer
                              data={(() => {
                                const safeParse = (data: any) => {
                                  if (!data || data === 'null' || data === '') return {};
                                  if (typeof data === 'string') {
                                    try {
                                      const parsed = JSON.parse(data);
                                      // If it's already an object, return it; if it's a string representation of an object, parse it again
                                      return typeof parsed === 'string' ? JSON.parse(parsed) : parsed;
                                    } catch {
                                      // If parsing fails, try to return an empty object or the original string
                                      return data.trim() !== '' ? { raw: data } : {};
                                    }
                                  }
                                  return data;
                                };
                                return safeParse(log.requestHeaders);
                              })()}
                              title="Headers"
                              collapsible={true}
                              defaultExpanded={false}
                            />
                            
                            {/* Request Body */}
                            {log.requestBody && (
                              <JsonViewer
                                data={(() => {
                                  const safeParse = (data: any) => {
                                    if (!data) return null;
                                    if (typeof data === 'string') {
                                      try {
                                        return JSON.parse(data);
                                      } catch {
                                        return data;
                                      }
                                    }
                                    return data;
                                  };
                                  return safeParse(log.requestBody);
                                })()}
                                title="Body"
                                collapsible={true}
                                defaultExpanded={false}
                              />
                            )}
                          </div>
                          
                          {/* Request Metadata */}
                          <div className="bg-gray-100 dark:bg-gray-700 p-3 rounded-lg">
                            <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
                              <div>
                                <span className="font-medium text-gray-600 dark:text-gray-400">Method:</span>
                                <br />
                                <span className="font-mono text-gray-800 dark:text-gray-200">{log.httpMethod}</span>
                              </div>
                              <div>
                                <span className="font-medium text-gray-600 dark:text-gray-400">Path:</span>
                                <br />
                                <span className="font-mono text-gray-800 dark:text-gray-200">{log.requestPath}</span>
                              </div>
                              <div>
                                <span className="font-medium text-gray-600 dark:text-gray-400">Timestamp:</span>
                                <br />
                                <span className="text-gray-800 dark:text-gray-200">{new Date(log.createdAt || Date.now()).toLocaleString()}</span>
                              </div>
                              <div>
                                <span className="font-medium text-gray-600 dark:text-gray-400">Client IP:</span>
                                <br />
                                <span className="font-mono text-gray-800 dark:text-gray-200">{(log as any).clientIp || 'N/A'}</span>
                              </div>
                            </div>
                          </div>
                        </div>

                        {/* Response Section */}
                        <div className="space-y-3">
                          <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                            <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                            Response
                          </h4>
                          <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
                            {/* Response Headers */}
                            <JsonViewer
                              data={(() => {
                                const safeParse = (data: any) => {
                                  if (!data || data === 'null' || data === '') return {};
                                  if (typeof data === 'string') {
                                    try {
                                      const parsed = JSON.parse(data);
                                      // If it's already an object, return it; if it's a string representation of an object, parse it again
                                      return typeof parsed === 'string' ? JSON.parse(parsed) : parsed;
                                    } catch {
                                      // If parsing fails, try to return an empty object or the original string
                                      return data.trim() !== '' ? { raw: data } : {};
                                    }
                                  }
                                  return data;
                                };
                                return safeParse(log.responseHeaders);
                              })()}
                              title="Headers"
                              collapsible={true}
                              defaultExpanded={false}
                            />
                            
                            {/* Response Body */}
                            {log.responseBody && (
                              <JsonViewer
                                data={(() => {
                                  const safeParse = (data: any) => {
                                    if (!data) return null;
                                    if (typeof data === 'string') {
                                      try {
                                        return JSON.parse(data);
                                      } catch {
                                        return data;
                                      }
                                    }
                                    return data;
                                  };
                                  return safeParse(log.responseBody);
                                })()}
                                title="Body"
                                collapsible={true}
                                defaultExpanded={false}
                              />
                            )}
                          </div>
                          
                          {/* Response Metadata */}
                          <div className="bg-gray-100 dark:bg-gray-700 p-3 rounded-lg">
                            <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
                              <div>
                                <span className="font-medium text-gray-600 dark:text-gray-400">Status:</span>
                                <br />
                                <span className="font-mono text-gray-800 dark:text-gray-200">{log.responseStatusCode}</span>
                              </div>
                              <div>
                                <span className="font-medium text-gray-600 dark:text-gray-400">Response Time:</span>
                                <br />
                                <span className="font-mono text-gray-800 dark:text-gray-200">{(log as any).responseTimeMs ? `${(log as any).responseTimeMs}ms` : 'N/A'}</span>
                              </div>
                              <div>
                                <span className="font-medium text-gray-600 dark:text-gray-400">Operation ID:</span>
                                <br />
                                <span className="font-mono text-gray-800 dark:text-gray-200">{log.operationId || 'N/A'}</span>
                              </div>
                              <div>
                                <span className="font-medium text-gray-600 dark:text-gray-400">User Agent:</span>
                                <br />
                                <span className="font-mono text-gray-800 dark:text-gray-200 truncate">{(log as any).userAgent || 'N/A'}</span>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            );
          })}

          {/* Loading indicator */}
          {isLoadingMore && (
            <div className="flex justify-center py-4">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          )}

          {/* Sentinel for infinite scroll */}
          <div ref={sentinelRef} className="h-4" />

          {/* End of results */}
          {!hasMore && logs.length > 0 && (
            <div className="text-center py-4 text-sm text-muted-foreground">
              No more logs to load
            </div>
          )}
        </div>
      )}
    </div>
  );
}