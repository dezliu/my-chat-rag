import { GraphQLError } from 'graphql';

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export class RestClient {
  constructor(private readonly baseUrl: string) {}

  async get<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: 'GET' });
  }

  async post<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  }

  async put<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  }

  async delete<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: 'DELETE' });
  }

  private async request<T>(path: string, init: RequestInit): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    const response = await fetch(url, init);
    const text = await response.text();
    let json: ApiResponse<T>;
    try {
      json = JSON.parse(text) as ApiResponse<T>;
    } catch {
      throw new GraphQLError(`REST 响应非 JSON: ${response.status}`, {
        extensions: { code: response.status, path: url },
      });
    }
    if (json.code !== 0) {
      throw new GraphQLError(json.message || 'REST 业务错误', {
        extensions: { code: json.code },
      });
    }
    return json.data;
  }
}
