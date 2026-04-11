'use client';

import React, { createContext, useContext, useMemo } from 'react';
import { MockServerStandaloneClient } from '@/services/mockServerStandaloneClient';

interface StandaloneServerContextValue {
  baseUrl: string;
  client: MockServerStandaloneClient;
}

const StandaloneServerContext = createContext<StandaloneServerContextValue | null>(null);

/**
 * Resolve the mock server REST API base URL.
 * 1. Explicit `baseUrl` prop
 * 2. NEXT_PUBLIC_MOCK_SERVER_API_URL or NEXT_PUBLIC_API_BASE_URL (build-time for static export)
 * 3. Browser: Next.js dev on :3000 → default http://localhost:8080 (API on another port)
 * 4. Browser: same-origin (fat JAR or Docker on :8080) → window.location.origin
 */
function resolveApiBaseUrl(baseUrlProp?: string): string {
  if (baseUrlProp) {
    return baseUrlProp.replace(/\/$/, '');
  }
  const fromEnv =
    process.env.NEXT_PUBLIC_MOCK_SERVER_API_URL ||
    process.env.NEXT_PUBLIC_API_BASE_URL ||
    process.env.NEXT_PUBLIC_STANDALONE_URL;
  if (typeof window === 'undefined') {
    return (fromEnv ?? 'http://localhost:8080').replace(/\/$/, '');
  }
  if (fromEnv) {
    return fromEnv.replace(/\/$/, '');
  }
  const { protocol, hostname, port } = window.location;
  if (port === '3000' || port === '3001') {
    return `${protocol}//${hostname}:8080`;
  }
  return window.location.origin.replace(/\/$/, '');
}

export function StandaloneServerProvider({
  baseUrl,
  children,
}: {
  baseUrl?: string;
  children: React.ReactNode;
}) {
  const resolvedUrl = useMemo(() => resolveApiBaseUrl(baseUrl), [baseUrl]);

  const client = useMemo(() => new MockServerStandaloneClient(resolvedUrl), [resolvedUrl]);
  const value = useMemo(() => ({ baseUrl: resolvedUrl, client }), [resolvedUrl, client]);

  return (
    <StandaloneServerContext.Provider value={value}>
      {children}
    </StandaloneServerContext.Provider>
  );
}

export function useStandaloneClient(): MockServerStandaloneClient {
  const ctx = useContext(StandaloneServerContext);
  if (!ctx) throw new Error('useStandaloneClient must be used inside <StandaloneServerProvider>');
  return ctx.client;
}

export function useStandaloneBaseUrl(): string {
  const ctx = useContext(StandaloneServerContext);
  if (!ctx) throw new Error('useStandaloneBaseUrl must be used inside <StandaloneServerProvider>');
  return ctx.baseUrl;
}
