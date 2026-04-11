// Utility function to generate an Idempotency-Key
const generateIdempotencyKey = () => crypto.randomUUID();

/**
 * Generates common headers for API requests
 * @param method - HTTP method (determines if Idempotency-Key is needed)
 * @param multipart - Whether the request is multipart/form-data
 * @returns Headers object
 */
export function getHeaders(authToken: string, method: "GET" | "POST" | "PUT" | "DELETE", multipart: boolean = false): HeadersInit {
  if (!authToken) {
    console.error("No auth token found!");
  }

  const headers: HeadersInit = {
    Authorization: `Bearer ${authToken}`
  };

  // Only set Content-Type if not multipart
  if (!multipart) {
    headers["Content-Type"] = "application/json";
  }

  // Add Idempotency-Key only for POST requests
  if (method === "POST") {
    headers["Idempotency-Key"] = generateIdempotencyKey();
  }

  return headers;
}

/**
 * Generates headers for API key authentication (for mock server endpoints)
 * @param apiKey - The API key for authentication
 * @param method - HTTP method (determines if Idempotency-Key is needed)
 * @param multipart - Whether the request is multipart/form-data
 * @returns Headers object with API key authentication
 */
export function getApiKeyHeaders(apiKey: string, method: "GET" | "POST" | "PUT" | "DELETE", multipart: boolean = false): HeadersInit {
  if (!apiKey || apiKey.trim() === '') {
    throw new Error("API key is required for mock server authentication");
  }

  const headers: HeadersInit = {
    'X-Mock-API-Key': apiKey.trim()
  };

  // Only set Content-Type if not multipart
  if (!multipart) {
    headers["Content-Type"] = "application/json";
  }

  // Add Idempotency-Key only for POST requests
  if (method === "POST") {
    headers["Idempotency-Key"] = generateIdempotencyKey();
  }

  return headers;
}
