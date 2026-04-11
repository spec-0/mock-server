/**
 * Open-source mock server UI — minimal shared config.
 * API calls use StandaloneServerContext; this is for non-React helpers that need a default base URL.
 */
export const config = {
  environment: (process.env.NODE_ENV || 'development') as string,
  apiBaseUrl:
    process.env.NEXT_PUBLIC_MOCK_SERVER_API_URL ||
    process.env.NEXT_PUBLIC_API_BASE_URL ||
    'http://localhost:8080',
} as const;

export function getEnvironmentInfo() {
  const env = config.environment;
  return {
    environment: env,
    isDevelopment: env === 'development',
    isProduction: env === 'production',
  };
}
