const rawBaseUrl = import.meta.env.VITE_ODAS_API_BASE;

export const API_BASE_URL = typeof rawBaseUrl === 'string' ? rawBaseUrl.trim() : '';
