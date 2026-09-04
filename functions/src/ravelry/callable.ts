import { HttpsError, type FunctionsErrorCode } from "firebase-functions/v2/https";

export interface BackendHttpError {
  readonly code: string;
  readonly httpStatus: number;
}

export function requireUid(auth: { uid?: string } | null | undefined): string {
  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "Firebase Auth is required for Ravelry backend calls.");
  }
  return auth.uid;
}

function isBackendHttpError(error: unknown): error is BackendHttpError {
  return (
    typeof error === "object" &&
    error !== null &&
    "code" in error &&
    "httpStatus" in error &&
    typeof (error as { code?: unknown }).code === "string" &&
    typeof (error as { httpStatus?: unknown }).httpStatus === "number"
  );
}

function functionsErrorCodeFor(httpStatus: number): FunctionsErrorCode {
  if (httpStatus === 400) {
    return "invalid-argument";
  }
  if (httpStatus === 401 || httpStatus === 403) {
    return "unauthenticated";
  }
  if (httpStatus === 404) {
    return "not-found";
  }
  if (httpStatus === 429) {
    return "resource-exhausted";
  }
  if (httpStatus >= 500) {
    return "unavailable";
  }
  return "failed-precondition";
}

export function httpsErrorFor(
  error: unknown,
  fallbackCode = "ravelry_backend_error",
): HttpsError {
  if (error instanceof HttpsError) {
    return error;
  }
  if (isBackendHttpError(error)) {
    return new HttpsError(functionsErrorCodeFor(error.httpStatus), error.code);
  }
  return new HttpsError("internal", fallbackCode);
}
