import { emptyGraph, parseServerGraph, serializeGraph } from '../runtime/graphCodec';
import type { JsonObject, PcgGraphDocument, PcgNodeSchema } from '../runtime/types';

export type BridgePreview = {
  available: boolean;
  blockCount?: number;
  severity?: string;
  stale?: boolean;
  issues?: Array<{ severity: string; message: string }>;
  nodeSummaries?: Array<{
    nodeId: string;
    type: string;
    order: number;
    inputCount: number;
    pointCount: number;
    volumeCount: number;
    plannedBlockDelta: number;
  }>;
  sampleBlocks?: Array<{ pos: number[]; state: string }>;
};

export type PackPreset = {
  id: string;
  name: string;
  type?: string;
  rule?: string;
  executor?: string;
};

export type PackSummary = {
  id: string;
  name: string;
  presets: PackPreset[];
};

export type EditorSession = {
  ok: boolean;
  sessionId: string;
  packId: string;
  presetId: string;
  packs: PackSummary[];
  selection: JsonObject;
  graph: PcgGraphDocument;
  preview: BridgePreview;
  devSaveAvailable: boolean;
};

export type BridgeResult = {
  ok: boolean;
  message?: string;
  preview?: BridgePreview;
  session?: EditorSession;
};

export type BridgeClient = {
  getSession(): Promise<EditorSession>;
  getNodeSchemas(): Promise<PcgNodeSchema[]>;
  selectPreset(packId: string, presetId: string): Promise<EditorSession>;
  requestPreview(presetId: string, graph: PcgGraphDocument, overrides?: JsonObject): Promise<BridgeResult>;
  saveGraph(packId: string, presetId: string, graph: PcgGraphDocument): Promise<BridgeResult>;
  bake(): Promise<BridgeResult>;
};

export function createBridgeClient(): BridgeClient {
  const params = new URLSearchParams(window.location.search);
  const token = params.get('token') ?? '';
  const explicitBridge = params.get('bridge');
  const baseUrl = explicitBridge ? explicitBridge.replace(/\/$/, '') : window.location.origin;

  async function call<T>(path: string, options: RequestInit = {}): Promise<T> {
    const headers = new Headers(options.headers);
    headers.set('Accept', 'application/json');
    if (options.body && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }
    if (token) {
      headers.set('X-Blockwright-Token', token);
    }
    const response = await fetch(`${baseUrl}${path}${path.includes('?') ? '&' : '?'}token=${encodeURIComponent(token)}`, {
      ...options,
      headers
    });
    const body = await response.json().catch(() => ({ ok: false, message: response.statusText }));
    if (!response.ok) {
      throw new Error(body.message ?? `Bridge request failed: ${response.status}`);
    }
    return body as T;
  }

  return {
    async getSession() {
      const raw = await call<Record<string, unknown>>('/api/session');
      return normalizeSession(raw);
    },
    async getNodeSchemas() {
      const raw = await call<{ ok: boolean; schemas: PcgNodeSchema[] }>('/api/node-schemas');
      return Array.isArray(raw.schemas) ? raw.schemas : [];
    },
    async selectPreset(packId: string, presetId: string) {
      const raw = await call<Record<string, unknown>>('/api/session/select', {
        method: 'POST',
        body: JSON.stringify({ packId, presetId })
      });
      return normalizeSession(raw);
    },
    async requestPreview(presetId: string, graph: PcgGraphDocument, overrides: JsonObject = {}) {
      return call<BridgeResult>('/api/graph/preview', {
        method: 'POST',
        body: JSON.stringify({ presetId, graph: serializeGraph(graph), overrides })
      });
    },
    async saveGraph(packId: string, presetId: string, graph: PcgGraphDocument) {
      const raw = await call<Record<string, unknown>>('/api/graph/save', {
        method: 'POST',
        body: JSON.stringify({ packId, presetId, graph: serializeGraph(graph) })
      });
      const result = raw as BridgeResult & { session?: Record<string, unknown> };
      if (result.session) {
        return { ...result, session: normalizeSession(result.session) };
      }
      return result;
    },
    async bake() {
      return call<BridgeResult>('/api/graph/bake', { method: 'POST', body: JSON.stringify({}) });
    }
  };
}

function normalizeSession(raw: Record<string, unknown>): EditorSession {
  return {
    ok: Boolean(raw.ok),
    sessionId: typeof raw.sessionId === 'string' ? raw.sessionId : 'local',
    packId: typeof raw.packId === 'string' ? raw.packId : '',
    presetId: typeof raw.presetId === 'string' ? raw.presetId : '',
    packs: Array.isArray(raw.packs) ? raw.packs as PackSummary[] : [],
    selection: raw.selection && typeof raw.selection === 'object' ? raw.selection as JsonObject : {},
    graph: raw.graph ? parseServerGraph(raw.graph) : emptyGraph(),
    preview: raw.preview && typeof raw.preview === 'object' ? raw.preview as BridgePreview : { available: false },
    devSaveAvailable: Boolean(raw.devSaveAvailable)
  };
}
