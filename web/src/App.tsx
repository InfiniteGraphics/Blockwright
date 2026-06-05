import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Background,
  Controls,
  MiniMap,
  ReactFlow,
  ReactFlowProvider,
  addEdge,
  useEdgesState,
  useNodesState,
  useReactFlow,
  type Connection,
  type Viewport
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { createBridgeClient, type BridgePreview, type EditorSession } from './bridge/blockwrightBridge';
import { Inspector } from './components/Inspector';
import { NodeLibrary } from './components/NodeLibrary';
import { PcgNode } from './components/PcgNode';
import { RuntimePanel } from './components/RuntimePanel';
import { TopBar } from './components/TopBar';
import { flowToGraph, graphToFlow, type PcgFlowEdge, type PcgFlowNode } from './runtime/graphCodec';
import { PcgGraphRuntime } from './runtime/graphRuntime';
import type { JsonValue, PcgNodeSchema, RuntimeCompilation } from './runtime/types';
import './styles/app.css';

const nodeTypes = { pcgNode: PcgNode };
const bridge = createBridgeClient();
const MAX_HISTORY = 80;

export function App() {
  return (
    <ReactFlowProvider>
      <PcgEditorApp />
    </ReactFlowProvider>
  );
}

function PcgEditorApp() {
  const [session, setSession] = useState<EditorSession>();
  const [schemas, setSchemas] = useState<PcgNodeSchema[]>([]);
  const [nodes, setNodes, onNodesChange] = useNodesState<PcgFlowNode>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<PcgFlowEdge>([]);
  const [selectedNodeId, setSelectedNodeId] = useState<string>();
  const [dirty, setDirty] = useState(false);
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState('Starting web editor...');
  const [preview, setPreview] = useState<BridgePreview>();
  const [, setHistoryVersion] = useState(0);
  const historyRef = useRef<{ past: GraphSnapshot[]; future: GraphSnapshot[] }>({ past: [], future: [] });
  const nodesRef = useRef<PcgFlowNode[]>([]);
  const edgesRef = useRef<PcgFlowEdge[]>([]);
  const reactFlow = useReactFlow<PcgFlowNode, PcgFlowEdge>();

  const schemaByType = useMemo(() => new Map(schemas.map((schema) => [schema.type, schema])), [schemas]);
  const runtime = useMemo(() => new PcgGraphRuntime(schemas), [schemas]);
  const selectedNode = useMemo(() => nodes.find((node) => node.id === selectedNodeId), [nodes, selectedNodeId]);
  const graph = useMemo(() => flowToGraph(nodes, edges, reactFlow.getViewport()), [edges, nodes, reactFlow]);
  const compilation: RuntimeCompilation | undefined = useMemo(() => schemas.length > 0 ? runtime.compile(graph) : undefined, [graph, runtime, schemas.length]);
  const canUndo = historyRef.current.past.length > 0;
  const canRedo = historyRef.current.future.length > 0;

  useEffect(() => {
    nodesRef.current = nodes;
    edgesRef.current = edges;
  }, [edges, nodes]);

  const bumpHistory = useCallback(() => setHistoryVersion((version) => version + 1), []);

  const recordSnapshot = useCallback(() => {
    const snapshot = makeSnapshot(nodesRef.current, edgesRef.current);
    const history = historyRef.current;
    const previous = history.past[history.past.length - 1];
    if (previous && snapshotKey(previous) === snapshotKey(snapshot)) {
      return;
    }
    history.past.push(snapshot);
    if (history.past.length > MAX_HISTORY) {
      history.past.shift();
    }
    history.future = [];
    bumpHistory();
  }, [bumpHistory]);

  const restoreSnapshot = useCallback((snapshot: GraphSnapshot) => {
    setNodes(snapshot.nodes);
    setEdges(snapshot.edges);
    nodesRef.current = snapshot.nodes;
    edgesRef.current = snapshot.edges;
    setSelectedNodeId(undefined);
    setDirty(true);
    bumpHistory();
  }, [bumpHistory, setEdges, setNodes]);

  const handleUndo = useCallback(() => {
    const history = historyRef.current;
    const snapshot = history.past.pop();
    if (!snapshot) {
      return;
    }
    history.future.push(makeSnapshot(nodesRef.current, edgesRef.current));
    restoreSnapshot(snapshot);
  }, [restoreSnapshot]);

  const handleRedo = useCallback(() => {
    const history = historyRef.current;
    const snapshot = history.future.pop();
    if (!snapshot) {
      return;
    }
    history.past.push(makeSnapshot(nodesRef.current, edgesRef.current));
    restoreSnapshot(snapshot);
  }, [restoreSnapshot]);

  const loadSession = useCallback(async () => {
    setBusy(true);
    setStatus('Loading MC session...');
    try {
      const [loadedSession, loadedSchemas] = await Promise.all([bridge.getSession(), bridge.getNodeSchemas()]);
      setSchemas(loadedSchemas);
      const flow = graphToFlow(loadedSession.graph, loadedSchemas);
      setNodes(flow.nodes);
      setEdges(flow.edges);
      nodesRef.current = flow.nodes;
      edgesRef.current = flow.edges;
      historyRef.current = { past: [], future: [] };
      bumpHistory();
      setSession(loadedSession);
      setPreview(loadedSession.preview);
      setDirty(false);
      setStatus('Connected to Blockwright bridge.');
      requestAnimationFrame(() => {
        if (flow.viewport) {
          reactFlow.setViewport(flow.viewport, { duration: 120 });
        } else if (flow.nodes.length > 0) {
          reactFlow.fitView({ padding: 0.28, duration: 120 });
        }
      });
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Failed to load session.');
    } finally {
      setBusy(false);
    }
  }, [bumpHistory, reactFlow, setEdges, setNodes]);

  useEffect(() => {
    void loadSession();
  }, [loadSession]);

  const markDirty = useCallback(() => setDirty(true), []);

  const handleConnect = useCallback((connection: Connection) => {
    if (!isValidConnection(connection, schemaByType, nodesRef.current)) {
      setStatus('Connection ports are not compatible.');
      return;
    }
    const nextEdge: PcgFlowEdge = {
      id: `edge_${connection.source}_${connection.sourceHandle ?? 'output'}_${connection.target}_${connection.targetHandle ?? 'input'}`,
      source: connection.source,
      target: connection.target,
      sourceHandle: connection.sourceHandle ?? 'output',
      targetHandle: connection.targetHandle ?? 'input',
      type: 'smoothstep',
      data: {
        fromPort: connection.sourceHandle ?? 'output',
        toPort: connection.targetHandle ?? 'input'
      }
    };
    recordSnapshot();
    setEdges((currentEdges) => {
      if (currentEdges.some((edge) => edge.source === nextEdge.source
        && edge.target === nextEdge.target
        && edge.sourceHandle === nextEdge.sourceHandle
        && edge.targetHandle === nextEdge.targetHandle)) {
        return currentEdges;
      }
      return addEdge(nextEdge, currentEdges);
    });
    markDirty();
  }, [markDirty, recordSnapshot, schemaByType, setEdges]);

  const handleAddNode = useCallback((type: string) => {
    const schema = schemaByType.get(type);
    const id = uniqueNodeId(type, nodes);
    const position = reactFlow.screenToFlowPosition({ x: window.innerWidth / 2, y: window.innerHeight / 2 });
    const config = Object.fromEntries((schema?.parameters ?? []).map((parameter) => [parameter.id, parameter.default ?? defaultFor(parameter.type)]));
    const node: PcgFlowNode = {
      id,
      type: 'pcgNode',
      position,
      data: {
        pcgId: id,
        pcgType: type,
        label: schema?.displayName ?? type,
        category: schema?.category ?? 'Other',
        description: schema?.description,
        outputType: schema?.outputs?.[0]?.dataType ?? 'any',
        inputType: schema?.inputs?.[0]?.dataType ?? 'any',
        inputs: schema?.inputs ?? [],
        outputs: schema?.outputs ?? [{ id: 'output', dataType: 'any' }],
        config,
        schema
      }
    };
    recordSnapshot();
    setNodes((currentNodes) => [...currentNodes, node]);
    setSelectedNodeId(id);
    markDirty();
  }, [markDirty, nodes, reactFlow, recordSnapshot, schemaByType, setNodes]);

  const handleConfigChange = useCallback((nodeId: string, key: string, value: JsonValue) => {
    recordSnapshot();
    setNodes((currentNodes) => currentNodes.map((node) => node.id === nodeId
      ? { ...node, data: { ...node.data, config: { ...node.data.config, [key]: value } } }
      : node));
    markDirty();
  }, [markDirty, recordSnapshot, setNodes]);

  const handleDeleteNode = useCallback((nodeId: string) => {
    recordSnapshot();
    setNodes((currentNodes) => currentNodes.filter((node) => node.id !== nodeId));
    setEdges((currentEdges) => currentEdges.filter((edge) => edge.source !== nodeId && edge.target !== nodeId));
    setSelectedNodeId(undefined);
    markDirty();
  }, [markDirty, recordSnapshot, setEdges, setNodes]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Delete' && event.key !== 'Backspace') {
        return;
      }
      const activeElement = document.activeElement;
      if (activeElement instanceof HTMLInputElement || activeElement instanceof HTMLTextAreaElement || activeElement instanceof HTMLSelectElement) {
        return;
      }
      const selectedNodes = nodes.filter((node) => node.selected).map((node) => ({ id: node.id }));
      const selectedEdges = edges.filter((edge) => edge.selected).map((edge) => ({ id: edge.id }));
      if (selectedNodes.length === 0 && selectedEdges.length === 0) {
        return;
      }
      event.preventDefault();
      recordSnapshot();
      void reactFlow.deleteElements({ nodes: selectedNodes, edges: selectedEdges }).then(() => {
        if (selectedNodes.some((node) => node.id === selectedNodeId)) {
          setSelectedNodeId(undefined);
        }
        markDirty();
      });
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [edges, markDirty, nodes, reactFlow, recordSnapshot, selectedNodeId]);

  useEffect(() => {
    const handleEditShortcuts = (event: KeyboardEvent) => {
      const activeElement = document.activeElement;
      if (activeElement instanceof HTMLInputElement || activeElement instanceof HTMLTextAreaElement || activeElement instanceof HTMLSelectElement) {
        return;
      }
      const key = event.key.toLowerCase();
      if (event.ctrlKey && key === 'z' && !event.shiftKey) {
        event.preventDefault();
        handleUndo();
        return;
      }
      if ((event.ctrlKey && key === 'y') || (event.ctrlKey && event.shiftKey && key === 'z')) {
        event.preventDefault();
        handleRedo();
        return;
      }
      if (event.ctrlKey && key === 'c') {
        const selectedNodes = nodesRef.current.filter((node) => node.selected);
        if (selectedNodes.length > 0) {
          event.preventDefault();
          void navigator.clipboard?.writeText(JSON.stringify(toClipboardPayload(selectedNodes, edgesRef.current)));
        }
        return;
      }
      if (event.ctrlKey && key === 'v') {
        event.preventDefault();
        void navigator.clipboard?.readText().then((text) => {
          const payload = parseClipboardPayload(text);
          if (!payload || payload.nodes.length === 0) {
            return;
          }
          recordSnapshot();
          const idMap = new Map<string, string>();
          const generatedNodes: PcgFlowNode[] = [];
          const pastedNodes = payload.nodes.map((node) => {
            const nextId = uniqueNodeId(node.data.pcgType, [...nodesRef.current, ...generatedNodes]);
            idMap.set(node.id, nextId);
            const pastedNode: PcgFlowNode = {
              ...node,
              id: nextId,
              selected: true,
              position: { x: node.position.x + 42, y: node.position.y + 42 },
              data: { ...node.data, pcgId: nextId }
            };
            generatedNodes.push(pastedNode);
            return pastedNode;
          });
          const pastedEdges: PcgFlowEdge[] = payload.edges
            .filter((edge) => idMap.has(edge.source) && idMap.has(edge.target))
            .map((edge) => ({
              ...edge,
              id: `edge_${idMap.get(edge.source)}_${edge.sourceHandle ?? 'output'}_${idMap.get(edge.target)}_${edge.targetHandle ?? 'input'}`,
              source: idMap.get(edge.source)!,
              target: idMap.get(edge.target)!,
              selected: true
            }));
          setNodes((currentNodes) => [...currentNodes.map((node): PcgFlowNode => ({ ...node, selected: false })), ...pastedNodes]);
          setEdges((currentEdges) => [...currentEdges.map((edge): PcgFlowEdge => ({ ...edge, selected: false })), ...pastedEdges]);
          setSelectedNodeId(pastedNodes[pastedNodes.length - 1]?.id);
          markDirty();
        });
      }
    };

    window.addEventListener('keydown', handleEditShortcuts);
    return () => window.removeEventListener('keydown', handleEditShortcuts);
  }, [handleRedo, handleUndo, markDirty, recordSnapshot, setEdges, setNodes]);

  const handlePreview = useCallback(async () => {
    if (!session || !compilation) {
      return;
    }
    if (!compilation.ok) {
      setStatus('Fix graph runtime errors before preview.');
      return;
    }
    setBusy(true);
    setStatus('Requesting server-side preview...');
    try {
      const result = await bridge.requestPreview(session.presetId, graph);
      setPreview(result.preview);
      setStatus(result.message ?? (result.ok ? 'Preview updated in Minecraft.' : 'Preview failed.'));
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Preview request failed.');
    } finally {
      setBusy(false);
    }
  }, [compilation, graph, session]);

  const handleSave = useCallback(async () => {
    if (!session) {
      return;
    }
    setBusy(true);
    setStatus('Saving graph to pack...');
    try {
      const result = await bridge.saveGraph(session.packId, session.presetId, graph);
      if (result.session) {
        setSession(result.session);
      }
      setDirty(false);
      setStatus(result.message ?? 'Graph saved.');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Save failed.');
    } finally {
      setBusy(false);
    }
  }, [graph, session]);

  const handleBake = useCallback(async () => {
    setBusy(true);
    setStatus('Baking current MC preview...');
    try {
      const result = await bridge.bake();
      setPreview(result.preview);
      setStatus(result.message ?? (result.ok ? 'Bake complete.' : 'Bake failed.'));
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Bake failed.');
    } finally {
      setBusy(false);
    }
  }, []);

  const handleExport = useCallback(() => {
    downloadJson(graph, `${session?.presetId || 'blockwright'}.pcg.graph.json`);
    setStatus('Graph JSON exported.');
  }, [graph, session?.presetId]);

  const handleSelectPreset = useCallback(async (packId: string, presetId: string) => {
    setBusy(true);
    setStatus('Switching preset...');
    try {
      const nextSession = await bridge.selectPreset(packId, presetId);
      const loadedSchemas = schemas.length > 0 ? schemas : await bridge.getNodeSchemas();
      if (schemas.length === 0) {
        setSchemas(loadedSchemas);
      }
      const flow = graphToFlow(nextSession.graph, loadedSchemas);
      setSession(nextSession);
      setNodes(flow.nodes);
      setEdges(flow.edges);
      setPreview(nextSession.preview);
      setDirty(false);
      setSelectedNodeId(undefined);
      setStatus('Preset loaded.');
      requestAnimationFrame(() => reactFlow.fitView({ padding: 0.28, duration: 120 }));
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Could not switch preset.');
    } finally {
      setBusy(false);
    }
  }, [reactFlow, schemas, setEdges, setNodes]);

  return (
    <div className="app-shell">
      <TopBar
        session={session}
        dirty={dirty}
        busy={busy}
        onSelect={handleSelectPreset}
        onReload={loadSession}
        onPreview={handlePreview}
        onSave={handleSave}
        onBake={handleBake}
        onExport={handleExport}
        onUndo={handleUndo}
        onRedo={handleRedo}
        canUndo={canUndo}
        canRedo={canRedo}
      />
      <main className="workspace">
        <NodeLibrary schemas={schemas} onAdd={handleAddNode} />
        <section className="canvas-shell">
          <ReactFlow<PcgFlowNode, PcgFlowEdge>
            nodes={nodes}
            edges={edges}
            nodeTypes={nodeTypes}
            onNodesChange={(changes) => {
              if (changes.some((change) => change.type !== 'select')) {
                recordSnapshot();
              }
              onNodesChange(changes);
              if (changes.some((change) => change.type !== 'select')) {
                markDirty();
              }
            }}
            onEdgesChange={(changes) => {
              if (changes.some((change) => change.type !== 'select')) {
                recordSnapshot();
              }
              onEdgesChange(changes);
              if (changes.some((change) => change.type !== 'select')) {
                markDirty();
              }
            }}
            onConnect={handleConnect}
            isValidConnection={(connection) => isValidConnection({
              source: connection.source,
              target: connection.target,
              sourceHandle: connection.sourceHandle ?? null,
              targetHandle: connection.targetHandle ?? null
            }, schemaByType, nodes)}
            onNodeClick={(_, node) => setSelectedNodeId(node.id)}
            onPaneClick={() => setSelectedNodeId(undefined)}
            fitView
          >
            <Background gap={24} size={1} />
            <Controls />
            <MiniMap pannable zoomable />
          </ReactFlow>
        </section>
        <div className="right-rail">
          <Inspector node={selectedNode} onConfigChange={handleConfigChange} onDelete={handleDeleteNode} />
          <RuntimePanel compilation={compilation} preview={preview} status={status} />
        </div>
      </main>
    </div>
  );
}

function uniqueNodeId(type: string, nodes: PcgFlowNode[]): string {
  const base = type.replace(/[^a-zA-Z0-9_]/g, '_') || 'node';
  let index = nodes.length + 1;
  let id = `${base}_${index}`;
  const existing = new Set(nodes.map((node) => node.id));
  while (existing.has(id)) {
    index += 1;
    id = `${base}_${index}`;
  }
  return id;
}

function defaultFor(type: string): JsonValue {
  if (type === 'bool') {
    return false;
  }
  if (type === 'int' || type === 'number') {
    return 0;
  }
  return '';
}

function downloadJson(value: unknown, fileName: string) {
  const blob = new Blob([JSON.stringify(value, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}

type GraphSnapshot = {
  nodes: PcgFlowNode[];
  edges: PcgFlowEdge[];
};

type ClipboardPayload = {
  kind: 'blockwright-pcg-selection';
  nodes: PcgFlowNode[];
  edges: PcgFlowEdge[];
};

function makeSnapshot(nodes: PcgFlowNode[], edges: PcgFlowEdge[]): GraphSnapshot {
  return {
    nodes: cloneNodes(nodes),
    edges: cloneEdges(edges)
  };
}

function cloneNodes(nodes: PcgFlowNode[]): PcgFlowNode[] {
  return nodes.map((node) => ({
    ...node,
    position: { ...node.position },
    data: {
      ...node.data,
      config: { ...node.data.config },
      inputs: [...node.data.inputs],
      outputs: [...node.data.outputs]
    }
  }));
}

function cloneEdges(edges: PcgFlowEdge[]): PcgFlowEdge[] {
  return edges.map((edge) => ({
    ...edge,
    data: edge.data ? { ...edge.data } : undefined
  }));
}

function snapshotKey(snapshot: GraphSnapshot): string {
  return JSON.stringify({
    nodes: snapshot.nodes.map((node) => ({
      id: node.id,
      type: node.data.pcgType,
      position: node.position,
      config: node.data.config
    })),
    edges: snapshot.edges.map((edge) => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      sourceHandle: edge.sourceHandle,
      targetHandle: edge.targetHandle
    }))
  });
}

function toClipboardPayload(selectedNodes: PcgFlowNode[], edges: PcgFlowEdge[]): ClipboardPayload {
  const selectedNodeIds = new Set(selectedNodes.map((node) => node.id));
  return {
    kind: 'blockwright-pcg-selection',
    nodes: cloneNodes(selectedNodes),
    edges: cloneEdges(edges.filter((edge) => selectedNodeIds.has(edge.source) && selectedNodeIds.has(edge.target)))
  };
}

function parseClipboardPayload(raw: string): ClipboardPayload | null {
  try {
    const parsed = JSON.parse(raw) as Partial<ClipboardPayload>;
    if (parsed.kind !== 'blockwright-pcg-selection' || !Array.isArray(parsed.nodes) || !Array.isArray(parsed.edges)) {
      return null;
    }
    return {
      kind: 'blockwright-pcg-selection',
      nodes: parsed.nodes as PcgFlowNode[],
      edges: parsed.edges as PcgFlowEdge[]
    };
  } catch {
    return null;
  }
}

function isValidConnection(connection: Connection, schemaByType: Map<string, PcgNodeSchema>, nodes: PcgFlowNode[]): boolean {
  if (!connection.source || !connection.target || connection.source === connection.target) {
    return false;
  }
  const sourceNode = nodes.find((node) => node.id === connection.source);
  const targetNode = nodes.find((node) => node.id === connection.target);
  if (!sourceNode || !targetNode) {
    return false;
  }
  const sourceSchema = schemaByType.get(sourceNode.data.pcgType);
  const targetSchema = schemaByType.get(targetNode.data.pcgType);
  const output = resolveOutput(sourceSchema, connection.sourceHandle);
  const input = resolveInput(targetSchema, connection.targetHandle);
  if (!output || !input) {
    return false;
  }
  return output.dataType === 'any' || input.dataType === 'any' || output.dataType === input.dataType;
}

function resolveInput(schema: PcgNodeSchema | undefined, portId: string | null | undefined) {
  const inputs = schema?.inputs ?? [];
  if (inputs.length === 0) {
    return undefined;
  }
  return inputs.find((input) => input.id === (portId ?? inputs[0].id));
}

function resolveOutput(schema: PcgNodeSchema | undefined, portId: string | null | undefined) {
  const outputs = schema?.outputs ?? [];
  if (outputs.length === 0) {
    return undefined;
  }
  return outputs.find((output) => output.id === (portId ?? outputs[0].id));
}
