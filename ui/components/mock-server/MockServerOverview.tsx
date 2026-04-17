'use client';

import React from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { RefreshCw, TrendingUp, Clock, Users, CheckCircle2 } from 'lucide-react';
import { MockServer, DashboardData } from '@/services/mockServerAdminClient';
import { useStandaloneBaseUrl } from '@/context/StandaloneServerContext';

interface MockServerOverviewProps {
  mockServer: MockServer;
  dashboardData: DashboardData | null;
  onRefresh: () => void;
}

/** Overview tab — aligned with cloud platform {@link MockServerOverview} (summary + optional recent activity). */
export default function MockServerOverview({ mockServer, dashboardData, onRefresh }: MockServerOverviewProps) {
  const baseUrl = useStandaloneBaseUrl();
  const summary = dashboardData?.summary;

  const mockBaseUrl = `${baseUrl}/mock/${mockServer.mockServerId}`;

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Requests</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{summary?.totalRequests ?? 0}</div>
            <p className="text-xs text-muted-foreground">All time requests</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Success Rate</CardTitle>
            <CheckCircle2 className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {summary && summary.totalRequests > 0
                ? Math.round((summary.successfulRequests / summary.totalRequests) * 100)
                : 0}
              %
            </div>
            <p className="text-xs text-muted-foreground">{summary?.successfulRequests ?? 0} successful</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Avg Response Time</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{summary?.avgResponseTime ?? 0}ms</div>
            <p className="text-xs text-muted-foreground">Average response time</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Unique Operations</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{summary?.uniqueOperations ?? 0}</div>
            <p className="text-xs text-muted-foreground">Different endpoints called</p>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Mock Server Information</CardTitle>
            <CardDescription>Basic information about your mock server</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-sm font-medium">Status</span>
              <Badge variant={mockServer.isEnabled ? 'default' : 'secondary'}>
                {mockServer.isEnabled ? 'Enabled' : 'Disabled'}
              </Badge>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm font-medium">Spec ID</span>
              <code className="text-sm bg-muted px-2 py-1 rounded">{mockServer.apiId}</code>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm font-medium">Created At</span>
              <span className="text-sm">{new Date(mockServer.createdAt).toLocaleDateString()}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm font-medium">Last Updated</span>
              <span className="text-sm">{new Date(mockServer.updatedAt).toLocaleDateString()}</span>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Quick Actions</CardTitle>
            <CardDescription>Common operations for your mock server</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <Button variant="outline" className="w-full justify-start" onClick={onRefresh}>
              <RefreshCw className="h-4 w-4 mr-2" />
              Refresh Data
            </Button>

            <Button
              variant="outline"
              className="w-full justify-start"
              onClick={() => {
                void navigator.clipboard.writeText(mockBaseUrl);
              }}
            >
              <TrendingUp className="h-4 w-4 mr-2" />
              Copy mock base URL
            </Button>
          </CardContent>
        </Card>
      </div>

      {dashboardData?.recentAnalytics && dashboardData.recentAnalytics.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Recent Activity</CardTitle>
            <CardDescription>Latest analytics data for your mock server</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {dashboardData.recentAnalytics.slice(0, 5).map((analytics, index) => (
                <div key={index} className="flex items-center justify-between p-3 border rounded-lg">
                  <div className="flex items-center space-x-3">
                    <div className="w-2 h-2 bg-green-500 rounded-full" />
                    <div>
                      <p className="text-sm font-medium">
                        {analytics.mostPopularOperation || 'Unknown Operation'}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {new Date(analytics.date).toLocaleDateString()}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-medium">{analytics.totalRequests} requests</p>
                    <p className="text-xs text-muted-foreground">{analytics.avgResponseTimeMs}ms avg</p>
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
