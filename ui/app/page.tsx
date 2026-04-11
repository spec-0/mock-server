'use client';

/**
 * Single-page app entry point for the standalone mock server UI.
 *
 * Next.js `output: 'export'` generates one static `index.html`. Spring Boot's
 * `WebConfig` SPA fallback serves that `index.html` for any `/ui/**` path.
 * This component reads `window.location.pathname` at runtime to decide whether
 * to render the list view (`/ui/`) or the detail view (`/ui/{mockServerId}`).
 *
 * URL structure:
 *   /ui/                          → mock server list
 *   /ui/{uuid}                    → mock server detail (clean URL, works after refresh)
 */

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { Plus, Search, Server, Clock } from 'lucide-react';
import { useStandaloneClient } from '@/context/StandaloneServerContext';
import type { MockServer } from '@/services/mockServerAdminClient';
import StandaloneCreateServerDialog from '@/components/mock-server/StandaloneCreateServerDialog';
import StandaloneMockServerPage from '@/components/mock-server/StandaloneMockServerPage';

// UUID regex — 8-4-4-4-12 hex segments
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function getMockServerIdFromPath(): string | null {
  if (typeof window === 'undefined') return null;
  // Strip basePath prefix (/ui) and trailing slash, get last segment
  const segments = window.location.pathname.replace(/\/+$/, '').split('/').filter(Boolean);
  const last = segments[segments.length - 1];
  return last && UUID_RE.test(last) ? last : null;
}

function MockServerList() {
  const client = useStandaloneClient();
  const router = useRouter();

  const [servers, setServers] = useState<MockServer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  useEffect(() => {
    client
      .listMockServers()
      .then(setServers)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load servers.'))
      .finally(() => setLoading(false));
  }, [client]);

  const filtered = servers.filter((s) => {
    const q = query.trim().toLowerCase();
    return !q || s.mockServerName.toLowerCase().includes(q);
  });

  const handleCreated = (server: MockServer) => {
    setServers((prev) => [server, ...prev]);
    // Navigate to detail view after creation
    router.push(`/${server.mockServerId}`);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Mock Servers</h1>
          <p className="text-muted-foreground text-sm mt-1">
            Register an OpenAPI spec and get instant mock responses.
          </p>
        </div>
        <Button onClick={() => setShowCreate(true)}>
          <Plus className="h-4 w-4 mr-2" />
          New Mock Server
        </Button>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder="Search mock servers..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="pl-9"
        />
      </div>

      {loading && (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-36 w-full rounded-lg" />
          ))}
        </div>
      )}

      {!loading && error && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
          {error}
        </div>
      )}

      {!loading && !error && filtered.length === 0 && (
        <div className="flex flex-col items-center justify-center py-16 gap-4 text-center">
          <Server className="h-10 w-10 text-muted-foreground/40" />
          <div>
            <p className="font-medium">No mock servers yet</p>
            <p className="text-muted-foreground text-sm">
              Create one by uploading an OpenAPI spec.
            </p>
          </div>
          <Button onClick={() => setShowCreate(true)}>
            <Plus className="h-4 w-4 mr-2" />
            Create your first mock server
          </Button>
        </div>
      )}

      {!loading && !error && filtered.length > 0 && (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filtered.map((server) => (
            <Link key={server.mockServerId} href={`/${server.mockServerId}`}>
              <Card className="h-full hover:border-primary/50 hover:shadow-md transition-all cursor-pointer">
                <CardHeader className="pb-2">
                  <div className="flex items-start justify-between gap-2">
                    <CardTitle className="text-base leading-tight line-clamp-2">
                      {server.mockServerName}
                    </CardTitle>
                    <Badge
                      variant="outline"
                      className={
                        server.isEnabled
                          ? 'text-green-700 border-green-300 bg-green-50 shrink-0'
                          : 'text-red-700 border-red-300 bg-red-50 shrink-0'
                      }
                    >
                      {server.isEnabled ? 'Active' : 'Disabled'}
                    </Badge>
                  </div>
                  {server.mockServerId && (
                    <CardDescription className="font-mono text-xs truncate">
                      {server.mockServerId}
                    </CardDescription>
                  )}
                </CardHeader>
                <CardContent>
                  <div className="flex items-center gap-1 text-xs text-muted-foreground">
                    <Clock className="h-3 w-3" />
                    <span>
                      {server.createdAt
                        ? new Date(server.createdAt).toLocaleDateString()
                        : '—'}
                    </span>
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}

      <StandaloneCreateServerDialog
        open={showCreate}
        onOpenChange={setShowCreate}
        onCreated={handleCreated}
      />
    </div>
  );
}

export default function HomePage() {
  const [mockServerId, setMockServerId] = useState<string | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    // Read path client-side (window is only available in the browser)
    setMockServerId(getMockServerIdFromPath());
    setReady(true);

    // Listen for popstate (browser back/forward) and pushState to update view
    const onPopState = () => setMockServerId(getMockServerIdFromPath());
    window.addEventListener('popstate', onPopState);
    return () => window.removeEventListener('popstate', onPopState);
  }, []);

  if (!ready) {
    // Skeleton while detecting path (avoids layout flash)
    return (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {[1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-36 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (mockServerId) {
    return <StandaloneMockServerPage mockServerId={mockServerId} />;
  }

  return <MockServerList />;
}
