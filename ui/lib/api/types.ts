/**
 * Convenience types for the spec0 Mock Server API.
 * Schema shapes are generated in `./generated.ts` from `../openapi.yaml` — run `npm run generate-types`.
 */

import type { components } from './generated';

export type { components, operations, paths } from './generated';

export type ResponseStrategy = components['schemas']['ResponseStrategy'];
export type ApiSpec = components['schemas']['ApiSpec'];
export type MockServer = components['schemas']['MockServer'];
export type MockResponseVariant = components['schemas']['MockResponseVariant'];
export type MockRequestLog = components['schemas']['MockRequestLog'];
export type MockOperationConfig = components['schemas']['MockOperationConfig'];
export type MockServerExport = components['schemas']['MockServerExport'];

export type RegisterSpecRequest = components['schemas']['RegisterSpecRequest'];
export type CreateMockServerRequest = components['schemas']['CreateMockServerRequest'];
export type PatchMockServerRequest = components['schemas']['PatchMockServerRequest'];
export type VariantRequest = components['schemas']['VariantRequest'];
export type PatchOperationConfigRequest = components['schemas']['PatchOperationConfigRequest'];

/**
 * Parsed OpenAPI operation — used by the UI; not part of the management REST contract.
 */
export interface Operation {
  operationId: string;
  method: string;
  path: string;
  summary?: string;
  description?: string;
  tags?: string[];
  parameters?: OperationParameter[];
  requestBodySchema?: Record<string, unknown>;
  responses?: Record<string, OperationResponse>;
}

export interface OperationParameter {
  name: string;
  in: 'query' | 'header' | 'path' | 'cookie';
  required?: boolean;
  schema?: Record<string, unknown>;
  description?: string;
}

export interface OperationResponse {
  description?: string;
  content?: Record<string, unknown>;
}
