export interface ApiProblemPayload {
  title?: string;
  detail?: string;
  message?: string;
  status?: number;
  instance?: string;
  code?: string;
}

export class ApiClientError extends Error {
  readonly status: number;
  readonly statusText: string;
  readonly code?: string;
  readonly detail?: string;
  readonly title?: string;

  constructor(params: {
    message: string;
    status: number;
    statusText: string;
    code?: string;
    detail?: string;
    title?: string;
  }) {
    super(params.message);
    this.name = "ApiClientError";
    this.status = params.status;
    this.statusText = params.statusText;
    this.code = params.code;
    this.detail = params.detail;
    this.title = params.title;
  }
}

function defaultMessageForStatus(status: number): string {
  if (status === 401) return "Please sign in again to continue.";
  if (status === 403) return "You do not have permission to perform this action.";
  if (status === 404) return "The requested resource was not found.";
  if (status === 409) return "The request conflicted with current data. Please retry.";
  if (status >= 500) return "Something went wrong on our side. Please try again shortly.";
  return "We could not complete your request. Please try again.";
}

async function readProblemPayload(response: Response): Promise<ApiProblemPayload | undefined> {
  try {
    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
      return (await response.json()) as ApiProblemPayload;
    }
    const text = await response.text();
    if (text && text.trim()) {
      return { detail: text };
    }
  } catch {
    // ignore parsing issues and use fallback messaging
  }
  return undefined;
}

export async function createApiClientError(
  response: Response,
  fallbackMessage?: string
): Promise<ApiClientError> {
  const payload = await readProblemPayload(response);
  const message =
    payload?.detail ||
    payload?.message ||
    fallbackMessage ||
    defaultMessageForStatus(response.status);

  return new ApiClientError({
    message,
    status: response.status,
    statusText: response.statusText,
    code: payload?.code,
    detail: payload?.detail,
    title: payload?.title,
  });
}

export function getErrorMessage(error: unknown, fallbackMessage: string): string {
  if (error instanceof ApiClientError) return error.message || fallbackMessage;
  if (error instanceof Error) return error.message || fallbackMessage;
  if (typeof error === "string" && error.trim()) return error;
  return fallbackMessage;
}
