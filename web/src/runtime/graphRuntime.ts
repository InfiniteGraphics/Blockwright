import type {
  JsonObject,
  PcgDataType,
  PcgGraphDocument,
  PcgGraphEdge,
  PcgGraphNode,
  PcgNodeSchema,
  RuntimeCompilation,
  RuntimeDiagnostic,
  RuntimeNodeTrace
} from './types';

export class PcgGraphRuntime {
  private readonly schemas = new Map<string, PcgNodeSchema>();

  constructor(schemas: PcgNodeSchema[]) {
    for (const schema of schemas) {
      this.schemas.set(schema.type, schema);
    }
  }

  compile(input: PcgGraphDocument): RuntimeCompilation {
    const graph = normalizeGraph(input, this.schemas);
    const diagnostics: RuntimeDiagnostic[] = [];
    const nodeById = new Map<string, PcgGraphNode>();

    for (const node of graph.nodes) {
      if (nodeById.has(node.id)) {
        diagnostics.push({ severity: 'error', message: `Duplicate node id: ${node.id}`, nodeId: node.id });
      }
      nodeById.set(node.id, node);
      const schema = this.schemas.get(node.type);
      if (!schema) {
        diagnostics.push({ severity: 'error', message: `Unknown PCG node type: ${node.type}`, nodeId: node.id });
        continue;
      }
      for (const parameter of schema.parameters) {
        if (parameter.required && node.config[parameter.id] === undefined) {
          diagnostics.push({ severity: 'error', message: `Missing required parameter: ${parameter.id}`, nodeId: node.id });
        }
        const value = node.config[parameter.id];
        if (typeof value === 'number') {
          if (typeof parameter.min === 'number' && value < parameter.min) {
            diagnostics.push({ severity: 'warning', message: `${parameter.id} is below minimum ${parameter.min}`, nodeId: node.id });
          }
          if (typeof parameter.max === 'number' && value > parameter.max) {
            diagnostics.push({ severity: 'warning', message: `${parameter.id} is above maximum ${parameter.max}`, nodeId: node.id });
          }
        }
      }
    }

    const incoming = new Map<string, PcgGraphEdge[]>();
    const outgoing = new Map<string, PcgGraphEdge[]>();
    for (const edge of graph.edges) {
      const edgeId = edge.id ?? `${edge.from}->${edge.to}`;
      const from = nodeById.get(edge.from);
      const to = nodeById.get(edge.to);
      if (!from) {
        diagnostics.push({ severity: 'error', message: `Edge source does not exist: ${edge.from}`, edgeId });
      }
      if (!to) {
        diagnostics.push({ severity: 'error', message: `Edge target does not exist: ${edge.to}`, edgeId });
      }
      if (from && to) {
        const fromOutput = resolveOutput(this.schemas.get(from.type), edge.fromPort);
        const toInput = resolveInput(this.schemas.get(to.type), edge.toPort);
        const fromType = fromOutput?.dataType ?? 'any';
        const toInputType = toInput?.dataType ?? 'any';
        if (!fromOutput) {
          diagnostics.push({ severity: 'error', message: `Output port does not exist: ${edge.fromPort ?? 'output'}`, edgeId, nodeId: from.id });
        }
        if (!toInput) {
          diagnostics.push({ severity: 'error', message: `Input port does not exist: ${edge.toPort ?? 'input'}`, edgeId, nodeId: to.id });
        }
        if (fromOutput && toInput && !isCompatible(fromType, toInputType)) {
          diagnostics.push({ severity: 'warning', message: `Type mismatch: ${fromType} -> ${toInputType}`, edgeId, nodeId: to.id });
        }
        pushMap(incoming, to.id, edge);
        pushMap(outgoing, from.id, edge);
      }
    }

    for (const node of graph.nodes) {
      const schema = this.schemas.get(node.type);
      const requiredInputs = schema?.inputs.filter((input) => input.required !== false) ?? [];
      if (requiredInputs.length > 0 && (incoming.get(node.id)?.length ?? 0) === 0) {
        diagnostics.push({ severity: 'error', message: `Node requires an input: ${schema?.displayName ?? node.type}`, nodeId: node.id });
      }
    }

    const order = topologicalOrder(graph.nodes, outgoing, diagnostics);
    const traces: RuntimeNodeTrace[] = order.map((nodeId, index) => {
      const node = nodeById.get(nodeId)!;
      return {
        nodeId,
        type: node.type,
        order: index,
        outputType: firstOutputType(this.schemas.get(node.type)),
        inputCount: incoming.get(nodeId)?.length ?? 0,
        inputs: (incoming.get(nodeId) ?? []).map((edge) => edge.toPort ?? 'input'),
        outputs: (outgoing.get(nodeId) ?? []).map((edge) => edge.fromPort ?? 'output')
      };
    });

    return {
      ok: !diagnostics.some((diagnostic) => diagnostic.severity === 'error'),
      diagnostics,
      order,
      traces,
      graph
    };
  }
}

export function normalizeGraph(input: PcgGraphDocument, schemas: Map<string, PcgNodeSchema>): PcgGraphDocument {
  const nodes = (input.nodes ?? []).map((node, index) => {
    const schema = schemas.get(node.type);
    return {
      id: node.id || `node_${index + 1}`,
      type: node.type || 'unknown',
      config: withDefaults(schema, node.config ?? {}),
      x: typeof node.x === 'number' ? node.x : 120 + index * 220,
      y: typeof node.y === 'number' ? node.y : 120
    };
  });
  const edges = (input.edges ?? []).map((edge, index) => ({
    id: edge.id ?? `${edge.from}-${edge.to}-${index}`,
    from: edge.from,
    to: edge.to,
    fromPort: edge.fromPort,
    toPort: edge.toPort
  }));
  return {
    debug: input.debug ?? false,
    nodes,
    edges,
    viewport: input.viewport
  };
}

export function withDefaults(schema: PcgNodeSchema | undefined, config: JsonObject): JsonObject {
  const next: JsonObject = { ...config };
  if (!schema) {
    return next;
  }
  for (const parameter of schema.parameters) {
    if (next[parameter.id] === undefined && parameter.default !== undefined) {
      next[parameter.id] = parameter.default;
    }
  }
  return next;
}

function pushMap(map: Map<string, PcgGraphEdge[]>, key: string, edge: PcgGraphEdge) {
  const list = map.get(key) ?? [];
  list.push(edge);
  map.set(key, list);
}

function firstOutputType(schema: PcgNodeSchema | undefined): PcgDataType {
  return schema?.outputs?.[0]?.dataType ?? 'any';
}

function firstInputType(schema: PcgNodeSchema | undefined): PcgDataType {
  return schema?.inputs?.[0]?.dataType ?? 'any';
}

function resolveInput(schema: PcgNodeSchema | undefined, portId: string | undefined) {
  const inputs = schema?.inputs ?? [];
  if (inputs.length === 0) {
    return portId && portId !== 'input' ? undefined : undefined;
  }
  return inputs.find((input) => input.id === (portId ?? inputs[0].id));
}

function resolveOutput(schema: PcgNodeSchema | undefined, portId: string | undefined) {
  const outputs = schema?.outputs ?? [];
  if (outputs.length === 0) {
    return undefined;
  }
  return outputs.find((output) => output.id === (portId ?? outputs[0].id));
}

function isCompatible(from: PcgDataType, to: PcgDataType): boolean {
  return from === 'any' || to === 'any' || from === to;
}

function topologicalOrder(nodes: PcgGraphNode[], outgoing: Map<string, PcgGraphEdge[]>, diagnostics: RuntimeDiagnostic[]): string[] {
  const indegree = new Map<string, number>();
  for (const node of nodes) {
    indegree.set(node.id, 0);
  }
  for (const edges of outgoing.values()) {
    for (const edge of edges) {
      indegree.set(edge.to, (indegree.get(edge.to) ?? 0) + 1);
    }
  }
  const queue = nodes.filter((node) => (indegree.get(node.id) ?? 0) === 0).map((node) => node.id);
  const order: string[] = [];
  while (queue.length > 0) {
    const current = queue.shift()!;
    order.push(current);
    for (const edge of outgoing.get(current) ?? []) {
      const next = (indegree.get(edge.to) ?? 0) - 1;
      indegree.set(edge.to, next);
      if (next === 0) {
        queue.push(edge.to);
      }
    }
  }
  if (order.length !== nodes.length) {
    diagnostics.push({ severity: 'error', message: 'Graph contains a cycle. PCG graphs must be directed acyclic graphs.' });
    for (const node of nodes) {
      if (!order.includes(node.id)) {
        order.push(node.id);
      }
    }
  }
  return order;
}
