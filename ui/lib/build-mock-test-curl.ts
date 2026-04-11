/**
 * Build a shell-safe curl command that mirrors MockServerStandaloneClient.testEndpointWithHeaders:
 * URL = {apiBaseUrl}/mock/{mockServerId}{pathForFetch}
 */

export function shellSingleQuote(s: string): string {
  return `'${s.replace(/'/g, `'\\''`)}'`;
}

export interface BuildMockTestCurlOptions {
  apiBaseUrl: string;
  mockServerId: string;
  /** Same string passed as `path` to testEndpointWithHeaders (often starts with `/`). */
  pathForFetch: string;
  method: string;
  customHeaders: Record<string, string>;
  operationId?: string;
  /** Raw JSON body text (validated by caller). Only added for methods that may send a body. */
  requestBodyRaw?: string;
}

export function buildMockTestCurl(opts: BuildMockTestCurlOptions): string {
  const base = opts.apiBaseUrl.replace(/\/$/, '');
  const p = opts.pathForFetch.startsWith('/') ? opts.pathForFetch : `/${opts.pathForFetch}`;
  const url = `${base}/mock/${opts.mockServerId}${p}`;
  const method = (opts.method || 'GET').toUpperCase();

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...opts.customHeaders,
  };
  if (opts.operationId) {
    headers['X-Mock-Operation-Id'] = opts.operationId;
  }

  let bodyStr: string | undefined;
  const withBody = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method);
  if (withBody && opts.requestBodyRaw?.trim()) {
    bodyStr = opts.requestBodyRaw.trim();
  }

  const lines: string[] = [`curl -sS -X ${method} \\`, `  ${shellSingleQuote(url)}`];
  for (const [k, v] of Object.entries(headers)) {
    lines[lines.length - 1] += ' \\';
    lines.push(`  -H ${shellSingleQuote(`${k}: ${v}`)}`);
  }
  if (bodyStr !== undefined) {
    lines[lines.length - 1] += ' \\';
    lines.push(`  -d ${shellSingleQuote(bodyStr)}`);
  }
  return lines.join('\n');
}
