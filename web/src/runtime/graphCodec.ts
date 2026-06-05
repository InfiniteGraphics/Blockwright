import type { Edge, Node, Viewport } from '@xyflow/react';
import type { JsonObject, PcgGraphDocument, PcgGraphEdge, PcgGraphNode, PcgNodeSchema } from './types';
import { withDefaults } from './graphRuntime';

export type PcgNodeData = {
  pcgId: string;
  pcgType: string;
  label: string;
  category: string;
  description?: string;
  outputType: string;
  inputType: string;
  inputs: PcgNodeSchema['inputs'];
  outputs: PcgNodeSchema['outputs'];
  config: JsonObject;
  schema?: PcgNodeSchema;
};

export type PcgFlowNode = Node<PcgNodeData, 'pcgNode'>;
export type PcgFlowEdge = Edge<{ fromPort?: string; toPort?: string }>;

export function graphToFlow(graph: PcgGraphDocument, schemas: PcgNodeSchema[]): { nodes: PcgFlowNode[]; edges: PcgFlowEdge[]; viewport?: Viewport } {
  const schemaByType = new Map(schemas.map((schema) => [schema.type, schema]));
  const nodes = (graph.nodes ?? []).map((node, index): PcgFlowNode => {
    const schema = schemaByType.get(node.type);
    const position = {
      x: typeof node.x === 'number' ? node.x : 96 + index * 240,
      y: typeof node.y === 'number' ? node.y : 96
    };
    return {
      id: node.id,
      type: 'pcgNode',
      position,
      data: {
        pcgId: node.id,
        pcgType: node.type,
        label: schema?.displayName ?? humanize(node.type),
        category: schema?.category ?? 'Other',
        description: schema?.description,
        outputType: schema?.outputs?.[0]?.dataType ?? 'any',
        inputType: schema?.inputs?.[0]?.dataType ?? 'any',
        inputs: schema?.inputs ?? [],
        outputs: schema?.outputs ?? [{ id: 'output', dataType: 'any' }],
        config: withDefaults(schema, node.config ?? {}),
        schema
      }
    };
  });
  const edges = (graph.edges ?? []).map((edge, index): PcgFlowEdge => ({
    id: edge.id ?? `edge_${edge.from}_${edge.to}_${index}`,
    source: edge.from,
    target: edge.to,
    sourceHandle: edge.fromPort ?? 'output',
    targetHandle: edge.toPort ?? 'input',
    type: 'smoothstep',
    animated: false,
    data: {
      fromPort: edge.fromPort,
      toPort: edge.toPort
    }
  }));
  return { nodes, edges, viewport: graph.viewport };
}

export function flowToGraph(nodes: PcgFlowNode[], edges: PcgFlowEdge[], viewport?: Viewport): PcgGraphDocument {
  return {
    debug: false,
    nodes: nodes.map((node): PcgGraphNode => ({
      id: node.id,
      type: node.data.pcgType,
      config: node.data.config,
      x: Math.round(node.position.x),
      y: Math.round(node.position.y)
    })),
    edges: edges.map((edge): PcgGraphEdge => ({
      id: edge.id,
      from: edge.source,
      to: edge.target,
      fromPort: edge.sourceHandle ?? undefined,
      toPort: edge.targetHandle ?? undefined
    })),
    viewport
  };
}

export function parseServerGraph(raw: unknown): PcgGraphDocument {
  if (!raw || typeof raw !== 'object') {
    return emptyGraph();
  }
  const object = raw as Record<string, unknown>;
  const nodes = Array.isArray(object.nodes) ? object.nodes.map(parseNode).filter(Boolean) as PcgGraphNode[] : [];
  const edges = Array.isArray(object.edges) ? object.edges.map(parseEdge).filter(Boolean) as PcgGraphEdge[] : [];
  return {
    debug: Boolean(object.debug),
    nodes,
    edges,
    viewport: parseViewport(object.viewport)
  };
}

export function serializeGraph(graph: PcgGraphDocument): PcgGraphDocument {
  return {
    debug: graph.debug ?? false,
    nodes: graph.nodes.map((node) => ({
      id: node.id,
      type: node.type,
      x: node.x,
      y: node.y,
      config: node.config
    })),
    edges: graph.edges.map((edge) => ({
      id: edge.id,
      from: edge.from,
      to: edge.to,
      ...(edge.fromPort ? { fromPort: edge.fromPort } : {}),
      ...(edge.toPort ? { toPort: edge.toPort } : {})
    })),
    viewport: graph.viewport
  };
}

export function emptyGraph(): PcgGraphDocument {
  return { debug: false, nodes: [], edges: [] };
}

function parseNode(raw: unknown): PcgGraphNode | null {
  if (!raw || typeof raw !== 'object') {
    return null;
  }
  const node = raw as Record<string, unknown>;
  const id = typeof node.id === 'string' ? node.id : '';
  const type = typeof node.type === 'string' ? node.type : '';
  if (!id || !type) {
    return null;
  }
  const config: JsonObject = {};
  if (node.config && typeof node.config === 'object' && !Array.isArray(node.config)) {
    Object.assign(config, node.config as JsonObject);
  }
  for (const [key, value] of Object.entries(node)) {
    if (key !== 'id' && key !== 'type' && key !== 'config' && key !== 'x' && key !== 'y') {
      config[key] = value as never;
    }
  }
  return {
    id,
    type,
    config,
    x: typeof node.x === 'number' ? node.x : undefined,
    y: typeof node.y === 'number' ? node.y : undefined
  };
}

function parseEdge(raw: unknown): PcgGraphEdge | null {
  if (Array.isArray(raw)) {
    const from = typeof raw[0] === 'string' ? raw[0] : '';
    const to = typeof raw[1] === 'string' ? raw[1] : '';
    return from && to ? { from, to } : null;
  }
  if (!raw || typeof raw !== 'object') {
    return null;
  }
  const edge = raw as Record<string, unknown>;
  const from = typeof edge.from === 'string' ? edge.from : '';
  const to = typeof edge.to === 'string' ? edge.to : '';
  return from && to ? {
    id: typeof edge.id === 'string' ? edge.id : undefined,
    from,
    to,
    fromPort: typeof edge.fromPort === 'string' ? edge.fromPort : undefined,
    toPort: typeof edge.toPort === 'string' ? edge.toPort : undefined
  } : null;
}

function parseViewport(raw: unknown): Viewport | undefined {
  if (!raw || typeof raw !== 'object') {
    return undefined;
  }
  const viewport = raw as Record<string, unknown>;
  return typeof viewport.x === 'number' && typeof viewport.y === 'number' && typeof viewport.zoom === 'number'
    ? { x: viewport.x, y: viewport.y, zoom: viewport.zoom }
    : undefined;
}

function humanize(type: string): string {
  return type.split('_').filter(Boolean).map((part) => part.slice(0, 1).toUpperCase() + part.slice(1)).join(' ') || 'Unknown';
}
