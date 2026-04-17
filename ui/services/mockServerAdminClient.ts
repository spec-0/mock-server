/**
 * Type-only shim that makes the standalone UI components compile without modification.
 *
 * These types mirror the shape that MockServerStandaloneClient.normalize*() methods
 * produce, so all copied-from-platform components can import from here unchanged.
 *
 * There is no implementation here — all actual API calls go through
 * MockServerStandaloneClient (mockServerStandaloneClient.ts).
 */

export type ResponseStrategy = 'RANDOM' | 'SEQUENTIAL' | 'ROUND_ROBIN' | 'DEFAULT_ONLY';

/** Platform-shaped MockServer — used by all UI components */
export interface MockServer {
  mockServerId: string;
  /** In standalone mode this maps to specId */
  apiId: string;
  /** In standalone mode this maps to specName / server name */
  apiName: string;
  mockServerName: string;
  description?: string;
  ownerTeamId: string;
  isDefault: boolean;
  apiKey?: string;
  isEnabled: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  /** Standalone-only — direct access to strategy */
  defaultStrategy?: ResponseStrategy;
}

export interface MockServerConfig {
  defaultStrategy: ResponseStrategy;
  /** OFF = disabled; WARN = log only; STRICT = reject invalid request/response JSON */
  validationMode: 'OFF' | 'WARN' | 'STRICT';
  maxLogEntries: number;
  rateLimitPerMinute: number;
  maxVariantsPerOperation: number;
  maxTotalVariants: number;
}

export interface MockResponseVariant {
  variantId: string;
  mockServerId: string;
  operationId: string;
  responseName: string;
  statusCode: string;
  responseBody: string;
  headers?: Record<string, string>;
  isDefault: boolean;
  displayOrder: number;
  /** CEL expression for dynamic variants. When set, evaluated at request time. */
  celExpression?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MockRequestLog {
  logId: string;
  mockServerId: string;
  operationId: string;
  httpMethod: string;
  requestPath: string;
  requestBody?: string;
  requestHeaders?: string;
  responseStatusCode: string;
  responseHeaders?: string;
  responseBody?: string;
  variantId?: string;
  responseTimeMs: number;
  clientIp: string;
  userAgent?: string;
  createdAt: string;
}

export interface MockOperationConfig {
  operationConfigId: string;
  mockServerId: string;
  operationId: string;
  isEnabled: boolean;
  responseStrategy?: ResponseStrategy;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

export interface CreateVariantRequest {
  operationId: string;
  responseName: string;
  statusCode: string;
  responseBody: string;
  headers?: Record<string, string>;
  isDefault?: boolean;
  displayOrder?: number;
  /** Optional CEL expression. When set, this is a CEL variant. */
  celExpression?: string | null;
}

export interface MockServerEnvVar {
  envVarId: string;
  mockServerId: string;
  varKey: string;
  varValue: string;
  createdAt: string;
}

export interface MockServerMcpConfig {
  configId: string;
  mockServerId: string;
  mcpEnabled: boolean;
  maxVariantsPerOperation: number;
  maxTotalVariants: number;
}

export type UpdateVariantRequest = CreateVariantRequest;

export interface GeneratedRequest {
  mockUrl: string;
  requestBody?: string;
  requestHeaders?: Record<string, string>;
}

export interface GeneratedUrl {
  mockUrl: string;
}

/** Optional row shape for “Recent activity” on the overview (cloud may populate). */
export interface MockServerAnalytics {
  analyticsId: string;
  date: string;
  totalRequests: number;
  avgResponseTimeMs: number;
  mostPopularOperation?: string;
}

/**
 * Overview dashboard — same nested shape as cloud {@link DashboardData} so {@link MockServerOverview}
 * matches the platform mock server page.
 */
export interface DashboardData {
  mockServer: MockServer;
  recentAnalytics: MockServerAnalytics[];
  summary: {
    totalRequests: number;
    successfulRequests: number;
    failedRequests: number;
    avgResponseTime: number;
    uniqueOperations: number;
  };
}

/**
 * Stub class — only present so components that do `import type { MockServerAdminClient }`
 * can compile. All actual calls in standalone mode go through MockServerStandaloneClient.
 */
export declare class MockServerAdminClient {
  getAnalytics(mockServerId: string, from?: string, to?: string): Promise<any>;
}
