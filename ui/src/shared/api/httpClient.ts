import {API_BASE_URL} from '../config/env';

interface ApiErrorBody {
    message?: string;
    error?: string;
}

async function buildError(response: Response): Promise<Error> {
    const contentType = response.headers.get('Content-Type') ?? '';

    if (contentType.includes('application/json')) {
        try {
            const body = await response.json() as ApiErrorBody;
            const message = body.message || body.error;
            if (message && message.trim()) {
                return new Error(message);
            }
        } catch {
            // Fall through to text fallback below.
        }
    }

    const text = await response.text();
    return new Error(text || `HTTP ${response.status}`);
}

async function readJson<TResponse>(response: Response): Promise<TResponse> {
    const contentType = response.headers.get('Content-Type') ?? '';

    if (!contentType.includes('application/json')) {
        const text = await response.text();
        const preview = text.trim().slice(0, 120);
        throw new Error(
            preview.startsWith('<!doctype html>') || preview.startsWith('<html')
                ? 'API request returned HTML instead of JSON.'
                : `API request returned non-JSON response: ${preview || contentType || 'empty response'}`
        );
    }

    return await response.json() as TResponse;
}

export async function apiGet<TResponse>(path: string): Promise<TResponse> {
    const response = await fetch(`${API_BASE_URL}${path}`);
    if (!response.ok) {
        throw await buildError(response);
    }
    return await readJson<TResponse>(response);
}

export async function apiPost<TRequest, TResponse>(path: string, body?: TRequest): Promise<TResponse> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
        method: 'POST',
        headers: body === undefined ? undefined : {'Content-Type': 'application/json'},
        body: body === undefined ? undefined : JSON.stringify(body)
    });
    if (!response.ok) {
        throw await buildError(response);
    }
    return await readJson<TResponse>(response);
}

export async function apiPostVoid<TRequest = void>(path: string, body?: TRequest): Promise<void> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
        method: 'POST',
        headers: body === undefined ? undefined : {'Content-Type': 'application/json'},
        body: body === undefined ? undefined : JSON.stringify(body)
    });
    if (!response.ok) {
        throw await buildError(response);
    }
}
