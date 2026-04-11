'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { RefreshCw, TrendingUp, Clock, CheckCircle, XCircle } from 'lucide-react';
import type { MockServerAdminClient } from '@/services/mockServerAdminClient';

// Structural interface — allows both MockServerAdminClient and MockServerStandaloneClient
interface AnalyticsClient {
  getAnalytics(mockServerId: string, from?: string, to?: string): Promise<any>;
}

// Backend shape for GET /api/v1/mock-servers/{mockServerId}/analytics
// (see openapi-spec: MockServerAnalytics has totals + requestsByDay[])
type BackendRequestsByDay = {
  date: string; // YYYY-MM-DD
  count: number;
};

type BackendMockServerAnalytics = {
  totalRequests?: number;
  uniqueUsers?: number;
  averageResponseTime?: number;
  errorRate?: number; // backend spec does not clarify format; handle 0..1 or 0..100
  requestsByDay?: BackendRequestsByDay[];
};

const toDateInputValue = (d: Date) => {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

// Backend uses server-side `LocalDate.now()` (server timezone). Since the UI dates are
// interpreted in the browser timezone, we query a +/-1 day window to avoid "empty table"
// caused by timezone date-shifts.
const adjustDateInput = (dateStr: string, deltaDays: number) => {
  const [y, m, d] = dateStr.split('-').map(Number);
  const dt = new Date(y, m - 1, d);
  dt.setDate(dt.getDate() + deltaDays);
  return toDateInputValue(dt);
};

interface MockServerAnalyticsProps {
  mockServerId: string;
  apiClient: AnalyticsClient;
}

export default function MockServerAnalyticsComponent({ mockServerId, apiClient }: MockServerAnalyticsProps) {
  const today = toDateInputValue(new Date());
  const thirtyDaysAgo = toDateInputValue(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000));

  const [analytics, setAnalytics] = useState<BackendMockServerAnalytics | null>(null);
  const [from, setFrom] = useState(thirtyDaysAgo);
  const [to, setTo] = useState(today);
  const [isLoading, setIsLoading] = useState(false);

  const fetchAnalytics = useCallback(async () => {
    if (!mockServerId) return;
    setIsLoading(true);
    try {
      const adjustedFrom = from ? adjustDateInput(from, -1) : from;
      const adjustedTo = to ? adjustDateInput(to, 1) : to;

      const data = await apiClient.getAnalytics(mockServerId, adjustedFrom, adjustedTo);
      // Frontend previously assumed `data` was an array, but backend returns a single analytics object.
      setAnalytics((data ?? null) as unknown as BackendMockServerAnalytics);
    } catch (err) {
      console.error('Failed to load analytics:', err);
      setAnalytics(null);
    } finally {
      setIsLoading(false);
    }
  }, [mockServerId, apiClient, from, to]);

  useEffect(() => {
    fetchAnalytics();
  }, [fetchAnalytics]);

  const safeAnalytics: BackendMockServerAnalytics = analytics ?? {};
  const totalRequests = safeAnalytics.totalRequests ?? 0;

  const requestsByDay: BackendRequestsByDay[] = Array.isArray(safeAnalytics.requestsByDay)
    ? safeAnalytics.requestsByDay
    : [];

  const errorRateRaw = safeAnalytics.errorRate ?? 0;
  // Heuristic: if backend sends 0..100 treat as percent, else treat as 0..1 fraction
  const errorRateFraction = errorRateRaw > 1 ? errorRateRaw / 100 : errorRateRaw;
  const totalFailed = Math.max(0, Math.round(totalRequests * errorRateFraction));
  const totalSuccessful = Math.max(0, totalRequests - totalFailed);
  const avgResponseTime = safeAnalytics.averageResponseTime ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold">Analytics</h2>
          <p className="text-muted-foreground">Performance metrics and usage statistics</p>
        </div>
        <Button variant="outline" onClick={fetchAnalytics} disabled={isLoading}>
          <RefreshCw className={`h-4 w-4 mr-2 ${isLoading ? 'animate-spin' : ''}`} />
          Refresh
        </Button>
      </div>

      {/* Date Range Filter */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base">Date Range</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-end gap-4">
            <div className="space-y-1">
              <Label htmlFor="analytics-from">From</Label>
              <Input
                id="analytics-from"
                type="date"
                value={from}
                max={to}
                onChange={(e) => setFrom(e.target.value)}
                className="w-40"
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="analytics-to">To</Label>
              <Input
                id="analytics-to"
                type="date"
                value={to}
                min={from}
                max={today}
                onChange={(e) => setTo(e.target.value)}
                className="w-40"
              />
            </div>
            <Button variant="secondary" onClick={fetchAnalytics} disabled={isLoading}>
              Apply
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Requests</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalRequests}</div>
            <p className="text-xs text-muted-foreground">In selected range</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Success Rate</CardTitle>
            <CheckCircle className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {totalRequests > 0 ? Math.round((totalSuccessful / totalRequests) * 100) : 0}%
            </div>
            <p className="text-xs text-muted-foreground">{totalSuccessful} successful</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Failed Requests</CardTitle>
            <XCircle className="h-4 w-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalFailed}</div>
            <p className="text-xs text-muted-foreground">Failed requests</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Avg Response Time</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{Math.round(avgResponseTime)}ms</div>
            <p className="text-xs text-muted-foreground">Average response time</p>
          </CardContent>
        </Card>
      </div>

      {/* Analytics Details */}
      {requestsByDay.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <TrendingUp className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold mb-2">No Analytics Data</h3>
            <p className="text-muted-foreground text-center">
              No data for the selected date range. Try a different range or wait for requests.
            </p>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardHeader>
            <CardTitle>Daily Analytics</CardTitle>
            <CardDescription>Detailed analytics data by date</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {requestsByDay.map((analytic) => (
                <div key={analytic.date} className="border rounded-lg p-4">
                  <div className="flex items-center justify-between mb-3">
                    <div>
                      <h4 className="font-medium">{new Date(analytic.date).toLocaleDateString()}</h4>
                      <p className="text-sm text-muted-foreground">Requests on this day</p>
                    </div>
                    <div className="text-right">
                      <div className="text-lg font-semibold">{analytic.count}</div>
                      <div className="text-sm text-muted-foreground">requests</div>
                    </div>
                  </div>

                  <div className="grid grid-cols-3 gap-4 text-sm">
                    <div>
                      <span className="text-muted-foreground">Successful:</span>
                      <span className="ml-2 font-medium text-green-600">
                        {Math.max(0, analytic.count - Math.round(analytic.count * errorRateFraction))}
                      </span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Failed:</span>
                      <span className="ml-2 font-medium text-red-600">
                        {Math.max(0, Math.round(analytic.count * errorRateFraction))}
                      </span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Avg Time:</span>
                      <span className="ml-2 font-medium">{Math.round(avgResponseTime)}ms</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
