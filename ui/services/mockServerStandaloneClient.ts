/**
 * Mock Server Standalone Client
 *
 * Maps to the /mock-server/ REST API exposed by mock-server-core (standalone + platform).
 * No auth token required — the standalone server runs without authentication by default.
 *
 * This client intentionally mirrors the interface of MockServerAdminClient so that all
 * existing UI components (MockServerPage, MockServerEndpoints, etc.) work unchanged.
 */

import type {
  MockServer,
  MockServerConfig,
  MockResponseVariant,
  MockRequestLog,
  MockOperationConfig,
  MockServerEnvVar,
  MockServerMcpConfig,
  CreateVariantRequest,
  UpdateVariantRequest,
  GeneratedRequest,
  GeneratedUrl,
} from './mockServerAdminClient';
import type { Operation } from '@/lib/api/types';

export interface StandaloneCreateServerRequest {
  specId: string;
  name: string;
  defaultStrategy?: 'RANDOM' | 'ROUND_ROBIN' | 'SEQUENTIAL' | 'DEFAULT_ONLY';
}

export interface StandaloneRegisterSpecRequest {
  specName: string;
  specContent: string;
}

export interface StandaloneSpec {
  specId: string;
  specName: string;
  specVersion: string;
  specHash: string;
  createdAt: string;
  updatedAt: string;
}

/** Maps legacy LENIENT → WARN; unknown values → OFF */
function normalizeSchemaValidationMode(raw: unknown): MockServerConfig['validationMode'] {
  const s = typeof raw === 'string' ? raw.trim().toUpperCase() : '';
  if (s === 'LENIENT') return 'WARN';
  if (s === 'OFF' || s === 'WARN' || s === 'STRICT') return s;
  return 'OFF';
}

function normalizeMockServer(obj: any): MockServer {
  return {
    mockServerId: obj.mockServerId,
    apiId: obj.specId ?? '',          // standalone has specId, not apiId
    apiName: obj.specName ?? obj.name ?? '',
    mockServerName: obj.name ?? obj.mockServerName ?? '',
    description: obj.description,
    ownerTeamId: '',                  // no team concept in standalone
    isDefault: obj.isDefault ?? false,
    apiKey: obj.apiKeyHash ?? '',
    isEnabled: obj.isEnabled ?? true,
    createdAt: obj.createdAt ?? '',
    updatedAt: obj.updatedAt ?? '',
    createdBy: '',
  };
}

function normalizeVariant(obj: any): MockResponseVariant {
  return {
    variantId: obj.variantId,
    mockServerId: obj.mockServerId,
    operationId: obj.operationId,
    responseName: obj.responseName,
    statusCode: obj.statusCode,
    responseBody: typeof obj.responseBody === 'object'
      ? JSON.stringify(obj.responseBody)
      : (obj.responseBody ?? ''),
    headers: obj.headers,
    isDefault: obj.isDefault ?? false,
    displayOrder: obj.displayOrder ?? 0,
    celExpression: obj.celExpression ?? null,
    createdAt: obj.createdAt ?? '',
    updatedAt: obj.updatedAt ?? '',
  };
}

function normalizeLog(obj: any): MockRequestLog {
  return {
    logId: obj.logId,
    mockServerId: obj.mockServerId,
    operationId: obj.operationId ?? '',
    httpMethod: obj.requestMethod ?? obj.httpMethod ?? 'GET',
    requestPath: obj.requestPath ?? '/',
    requestBody: obj.requestBody,
    requestHeaders: typeof obj.requestHeaders === 'object'
      ? JSON.stringify(obj.requestHeaders)
      : obj.requestHeaders,
    responseStatusCode: String(obj.responseStatusCode ?? obj.statusCode ?? '200'),
    responseHeaders: typeof obj.responseHeaders === 'object'
      ? JSON.stringify(obj.responseHeaders)
      : obj.responseHeaders,
    responseBody: obj.responseBody,
    variantId: obj.variantId,
    responseTimeMs: obj.responseTimeMs ?? 0,
    clientIp: obj.clientIp ?? '',
    userAgent: obj.userAgent,
    createdAt: obj.requestedAt ?? obj.createdAt ?? '',
  };
}

function normalizeOperationConfig(obj: any): MockOperationConfig {
  return {
    operationConfigId: obj.configId ?? obj.operationConfigId ?? '',
    mockServerId: obj.mockServerId,
    operationId: obj.operationId,
    isEnabled: obj.isEnabled ?? true,
    responseStrategy: obj.strategyOverride ?? obj.responseStrategy,
    createdAt: obj.createdAt ?? '',
    updatedAt: obj.updatedAt ?? '',
    createdBy: '',
  };
}

async function handleError(response: Response, context: string): Promise<never> {
  const fallback = `${context}: ${response.status} ${response.statusText}`;
  let message = fallback;
  try {
    const text = await response.text();
    if (text) {
      const ct = response.headers.get('content-type') ?? '';
      if (ct.includes('application/json')) {
        try {
          const j = JSON.parse(text) as {
            message?: string;
            error?: string;
            details?: string[];
          };
          const lines: string[] = [];
          if (j.message) lines.push(j.message);
          if (j.details?.length) lines.push(...j.details);
          if (lines.length > 0) {
            message = [context, ...lines].join('\n');
          } else if (j.error) {
            message = `${context}: ${j.error}`;
          } else {
            message = text;
          }
        } catch {
          message = text;
        }
      } else {
        message = text;
      }
    }
  } catch {
    /* ignore */
  }
  throw new Error(message);
}

/** Default API origin when no explicit base URL is passed (SSR / tests). */
export function getStandaloneMockServerDefaultBaseUrl(): string {
  const fromEnv =
    process.env.NEXT_PUBLIC_MOCK_SERVER_API_URL ||
    process.env.NEXT_PUBLIC_API_BASE_URL ||
    process.env.NEXT_PUBLIC_STANDALONE_URL;
  return (fromEnv ?? 'http://localhost:8080').replace(/\/$/, '');
}

export class MockServerStandaloneClient {
  private baseUrl: string;

  constructor(baseUrl?: string) {
    this.baseUrl = (baseUrl ?? getStandaloneMockServerDefaultBaseUrl()).replace(/\/$/, '');
  }

  private headers(method: string = 'GET'): HeadersInit {
    const h: Record<string, string> = { 'Content-Type': 'application/json' };
    return h;
  }

  // ── Spec registration ───────────────────────────────────────────────────────

  async registerSpec(request: StandaloneRegisterSpecRequest): Promise<StandaloneSpec> {
    const response = await fetch(`${this.baseUrl}/mock-server/specs`, {
      method: 'POST',
      headers: this.headers('POST'),
      body: JSON.stringify(request),
    });
    if (!response.ok) return handleError(response, 'Failed to register spec');
    return response.json();
  }

  async getSpec(specId: string): Promise<StandaloneSpec> {
    const response = await fetch(`${this.baseUrl}/mock-server/specs/${specId}`, {
      headers: this.headers(),
    });
    if (!response.ok) return handleError(response, 'Failed to get spec');
    return response.json();
  }

  // ── Server CRUD ──────────────────────────────────────────────────────────────

  async listMockServers(): Promise<MockServer[]> {
    const response = await fetch(`${this.baseUrl}/mock-server/servers`, {
      headers: this.headers(),
    });
    if (!response.ok) return handleError(response, 'Failed to list mock servers');
    const data = await response.json();
    return (Array.isArray(data) ? data : []).map(normalizeMockServer);
  }

  async getMockServer(mockServerId: string): Promise<MockServer> {
    const response = await fetch(`${this.baseUrl}/mock-server/servers/${mockServerId}`, {
      headers: this.headers(),
    });
    if (!response.ok) return handleError(response, 'Failed to get mock server');
    return normalizeMockServer(await response.json());
  }

  async createMockServer(request: StandaloneCreateServerRequest): Promise<MockServer> {
    const response = await fetch(`${this.baseUrl}/mock-server/servers`, {
      method: 'POST',
      headers: this.headers('POST'),
      body: JSON.stringify(request),
    });
    if (!response.ok) return handleError(response, 'Failed to create mock server');
    return normalizeMockServer(await response.json());
  }

  async updateMockServer(mockServerId: string, updates: { name?: string; isEnabled?: boolean; defaultStrategy?: string }): Promise<MockServer> {
    const response = await fetch(`${this.baseUrl}/mock-server/servers/${mockServerId}`, {
      method: 'PATCH',
      headers: this.headers('PATCH'),
      body: JSON.stringify(updates),
    });
    if (!response.ok) return handleError(response, 'Failed to update mock server');
    return normalizeMockServer(await response.json());
  }

  async deleteMockServer(mockServerId: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/mock-server/servers/${mockServerId}`, {
      method: 'DELETE',
      headers: this.headers('DELETE'),
    });
    if (!response.ok) return handleError(response, 'Failed to delete mock server');
  }

  // ── enable / disable shim (mirrors MockServerAdminClient.performMockServerAction) ─

  async performMockServerAction(
    mockServerId: string,
    action: 'enable' | 'disable',
  ): Promise<MockServer> {
    return this.updateMockServer(mockServerId, { isEnabled: action === 'enable' });
  }

  // ── Variants ────────────────────────────────────────────────────────────────

  async getVariants(mockServerId: string, operationId?: string): Promise<MockResponseVariant[]> {
    const url = operationId
      ? `${this.baseUrl}/mock-server/servers/${mockServerId}/variants?operationId=${encodeURIComponent(operationId)}`
      : `${this.baseUrl}/mock-server/servers/${mockServerId}/variants`;
    const response = await fetch(url, { headers: this.headers() });
    if (!response.ok) return handleError(response, 'Failed to get variants');
    const data = await response.json();
    return (Array.isArray(data) ? data : []).map(normalizeVariant);
  }

  async createVariant(mockServerId: string, request: CreateVariantRequest): Promise<MockResponseVariant> {
    const response = await fetch(`${this.baseUrl}/mock-server/servers/${mockServerId}/variants`, {
      method: 'POST',
      headers: this.headers('POST'),
      body: JSON.stringify(request),
    });
    if (!response.ok) return handleError(response, 'Failed to create variant');
    return normalizeVariant(await response.json());
  }

  async updateVariant(
    mockServerId: string,
    variantId: string,
    request: UpdateVariantRequest,
  ): Promise<MockResponseVariant> {
    const response = await fetch(
      `${this.baseUrl}/mock-server/servers/${mockServerId}/variants/${variantId}`,
      {
        method: 'PUT',
        headers: this.headers('PUT'),
        body: JSON.stringify(request),
      },
    );
    if (!response.ok) return handleError(response, 'Failed to update variant');
    return normalizeVariant(await response.json());
  }

  async deleteVariant(mockServerId: string, variantId: string): Promise<void> {
    const response = await fetch(
      `${this.baseUrl}/mock-server/servers/${mockServerId}/variants/${variantId}`,
      { method: 'DELETE', headers: this.headers('DELETE') },
    );
    if (!response.ok) return handleError(response, 'Failed to delete variant');
  }

  // ── Logs ────────────────────────────────────────────────────────────────────

  async getLogs(mockServerId: string, limit = 50, _offset?: number): Promise<MockRequestLog[]> {
    const response = await fetch(
      `${this.baseUrl}/mock-server/servers/${mockServerId}/logs?limit=${limit}`,
      { headers: this.headers() },
    );
    if (!response.ok) return handleError(response, 'Failed to get logs');
    const data = await response.json();
    return (Array.isArray(data) ? data : []).map(normalizeLog);
  }

  // ── Operation configs ────────────────────────────────────────────────────────

  async getOperationConfigs(mockServerId: string): Promise<MockOperationConfig[]> {
    const response = await fetch(
      `${this.baseUrl}/mock-server/servers/${mockServerId}/operations`,
      { headers: this.headers() },
    );
    if (!response.ok) return handleError(response, 'Failed to get operation configs');
    const data = await response.json();
    return (Array.isArray(data) ? data : []).map(normalizeOperationConfig);
  }

  /**
   * Returns operations parsed from the spec (method, path, operationId).
   * Uses GET /mock-server/servers/{id}/spec-operations — backed by mock_server_operations table.
   */
  async getSpecOperations(mockServerId: string): Promise<Operation[]> {
    const response = await fetch(
      `${this.baseUrl}/mock-server/servers/${mockServerId}/spec-operations`,
      { headers: this.headers() },
    );
    if (!response.ok) return handleError(response, 'Failed to get spec operations');
    const data = await response.json();
    return (Array.isArray(data) ? data : []).map((op: any): Operation => ({
      operationId: op.operationId,
      method: (op.httpMethod ?? 'GET').toUpperCase(),
      path: op.path ?? '/',
      summary: op.summary,
      description: op.description,
      tags: op.tags,
    }));
  }

  async updateOperationConfig(
    mockServerId: string,
    operationId: string,
    updates: { isEnabled?: boolean; responseStrategy?: string },
  ): Promise<MockOperationConfig> {
    const response = await fetch(
      `${this.baseUrl}/mock-server/servers/${mockServerId}/operations/${encodeURIComponent(operationId)}`,
      {
        method: 'PATCH',
        headers: this.headers('PATCH'),
        body: JSON.stringify(updates),
      },
    );
    if (!response.ok) return handleError(response, 'Failed to update operation config');
    return normalizeOperationConfig(await response.json());
  }

  async toggleOperation(mockServerId: string, operationId: string, enabled: boolean): Promise<MockOperationConfig> {
    return this.updateOperationConfig(mockServerId, operationId, { isEnabled: enabled });
  }

  // ── Stubs for platform-only features ─────────────────────────────────────────
  // These return sensible defaults so MockServerPage renders without crashing.

  async getMockServerConfig(mockServerId: string): Promise<MockServerConfig> {
    try {
      const response = await fetch(
        `${this.baseUrl}/mock-server/servers/${mockServerId}/config`,
        { headers: this.headers() },
      );
      if (!response.ok) return handleError(response, 'Failed to get mock server config');
      const c = await response.json();
      return {
        defaultStrategy: c.defaultStrategy ?? 'RANDOM',
        validationMode: normalizeSchemaValidationMode(c.schemaValidationMode),
        maxLogEntries: 200,
        rateLimitPerMinute: -1,
        maxVariantsPerOperation: c.maxVariantsPerOperation ?? 10,
        maxTotalVariants: c.maxTotalVariants ?? 100,
      };
    } catch {
      return {
        defaultStrategy: 'RANDOM',
        validationMode: 'OFF',
        maxLogEntries: 200,
        rateLimitPerMinute: -1,
        maxVariantsPerOperation: 10,
        maxTotalVariants: 100,
      };
    }
  }

  async updateMockServerConfig(mockServerId: string, config: MockServerConfig): Promise<MockServerConfig> {
    const response = await fetch(`${this.baseUrl}/mock-server/servers/${mockServerId}/config`, {
      method: 'PATCH',
      headers: this.headers('PATCH'),
      body: JSON.stringify({
        defaultStrategy: config.defaultStrategy,
        schemaValidationMode: config.validationMode,
        maxVariantsPerOperation: config.maxVariantsPerOperation,
        maxTotalVariants: config.maxTotalVariants,
      }),
    });
    if (!response.ok) return handleError(response, 'Failed to update mock server config');
    const c = await response.json();
    return {
      defaultStrategy: c.defaultStrategy ?? config.defaultStrategy,
      validationMode: normalizeSchemaValidationMode(c.schemaValidationMode ?? config.validationMode),
      maxLogEntries: config.maxLogEntries,
      rateLimitPerMinute: config.rateLimitPerMinute,
      maxVariantsPerOperation: c.maxVariantsPerOperation ?? config.maxVariantsPerOperation,
      maxTotalVariants: c.maxTotalVariants ?? config.maxTotalVariants,
    };
  }

  async syncOperations(_mockServerId: string): Promise<void> {
    // No-op: operations are derived from spec at registration time.
  }

  async generateRequest(_mockServerId: string, operationId: string): Promise<GeneratedRequest> {
    return {
      mockUrl: `/mock/${_mockServerId}/${operationId}`,
    };
  }

  async generateUrl(_mockServerId: string, operationId: string): Promise<GeneratedUrl> {
    return {
      mockUrl: `/mock/${_mockServerId}/${operationId}`,
    };
  }

  // ── Environment variables ────────────────────────────────────────────────

  async getEnvVars(mockServerId: string): Promise<MockServerEnvVar[]> {
    const response = await fetch(
      `${this.baseUrl}/mock-server/servers/${mockServerId}/env-vars`,
      { headers: this.headers() },
    );
    if (!response.ok) return handleError(response, 'Failed to get env vars');
    const data = await response.json();
    return Array.isArray(data) ? data : [];
  }

  async createEnvVar(mockServerId: string, varKey: string, varValue: string): Promise<MockServerEnvVar> {
    const response = await fetch(
      `${this.baseUrl}/mock-server/servers/${mockServerId}/env-vars`,
      {
        method: 'POST',
        headers: this.headers('POST'),
        body: JSON.stringify({ varKey, varValue }),
      },
    );
    if (!response.ok) return handleError(response, 'Failed to create env var');
    return response.json();
  }

  async deleteEnvVar(mockServerId: string, envVarId: string): Promise<void> {
    const response = await fetch(
      `${this.baseUrl}/mock-server/servers/${mockServerId}/env-vars/${envVarId}`,
      { method: 'DELETE', headers: this.headers('DELETE') },
    );
    if (!response.ok) return handleError(response, 'Failed to delete env var');
  }

  // ── MCP config ──────────────────────────────────────────────────────────

  async getMcpConfig(mockServerId: string): Promise<MockServerMcpConfig | null> {
    try {
      const response = await fetch(
        `${this.baseUrl}/mock-server/servers/${mockServerId}/mcp-config`,
        { headers: this.headers() },
      );
      if (!response.ok) return null;
      return response.json();
    } catch {
      return null;
    }
  }

  async updateMcpConfig(mockServerId: string, mcpEnabled: boolean): Promise<MockServerMcpConfig | null> {
    try {
      const response = await fetch(
        `${this.baseUrl}/mock-server/servers/${mockServerId}/mcp-config`,
        {
          method: 'PATCH',
          headers: this.headers('PATCH'),
          body: JSON.stringify({ mcpEnabled }),
        },
      );
      if (!response.ok) return null;
      return response.json();
    } catch {
      return null;
    }
  }

  async getAnalytics(mockServerId: string, from?: string, to?: string): Promise<unknown> {
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    const q = params.toString();
    const url = `${this.baseUrl}/mock-server/servers/${mockServerId}/analytics${q ? `?${q}` : ''}`;
    const response = await fetch(url, { headers: this.headers() });
    if (!response.ok) return handleError(response, 'Failed to load analytics');
    return response.json();
  }

  /**
   * Test an endpoint directly against the standalone mock server.
   * No API key required (standalone runs unauthenticated by default).
   *
   * @param path OpenAPI path only (e.g. {@code /cart/123}), not {@code /mock/{id}/...} — the client
   *     builds {@code {base}/mock/{mockServerId}{path}}.
   */
  async testEndpointWithHeaders(
    mockServerId: string,
    path: string,
    method = 'GET',
    body?: any,
    headers?: Record<string, string>,
    operationId?: string,
  ) {
    const url = `${this.baseUrl}/mock/${mockServerId}${path}`;
    const requestHeaders: Record<string, string> = {
      'Content-Type': 'application/json',
      ...headers,
    };
    if (operationId) requestHeaders['X-Mock-Operation-Id'] = operationId;

    const response = await fetch(url, {
      method: method.toUpperCase(),
      headers: requestHeaders,
      body: body ? JSON.stringify(body) : undefined,
    });

    const isMockResponse = response.headers.get('X-spec0-Mock-Response') === 'true';
    const mockVariantId = response.headers.get('X-spec0-Mock-Variant-Id') ?? undefined;
    const mockOperationId = response.headers.get('X-spec0-Mock-Operation-Id') ?? undefined;

    const responseHeaders: Record<string, string> = {};
    response.headers.forEach((value, key) => { responseHeaders[key] = value; });

    let data: any;
    try { data = await response.json(); }
    catch { data = await response.text().catch(() => ''); }

    return { data, status: response.status, statusText: response.statusText, headers: responseHeaders, isMockResponse, mockVariantId, mockOperationId };
  }
}
