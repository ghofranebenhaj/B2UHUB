export function extractApiError(err: unknown, fallback: string): string {
  if (!err || typeof err !== 'object') return fallback;
  const e = err as { error?: { detail?: string; title?: string }; message?: string; status?: number };
  if (e.error?.detail) return e.error.detail;
  if (e.error?.title) return e.error.title;
  if (e.status === 0) return 'Backend inaccessible — démarrez Spring Boot sur le port 8080.';
  if (e.status === 403) return 'Accès refusé (403) — redémarrez le backend après mise à jour.';
  if (e.message) return e.message;
  return fallback;
}
