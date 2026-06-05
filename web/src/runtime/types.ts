export type JsonPrimitive = string | number | boolean | null;
export type JsonValue = JsonPrimitive | JsonObject | JsonValue[];
export type JsonObject = { [key: string]: JsonValue };

export type PcgParamKind = 'string' | 'int' | 'number' | 'bool' | 'block' | 'enum' | string;
export type PcgDataType = 'any' | 'volume' | 'spline' | 'points' | 'blocks' | 'modules' | string;

export type PcgParameterSchema = {
  id: string;
  type: PcgParamKind;
  default?: JsonValue;
  description?: string;
  required?: boolean;
  min?: number;
  max?: number;
  options?: string[];
};

export type PcgPortSchema = {
  id: string;
  dataType: PcgDataType;
  label?: string;
  required?: boolean;
};

export type PcgNodeSchema = {
  type: string;
  displayName: string;
  category: string;
  description?: string;
  inputs: PcgPortSchema[];
  outputs: PcgPortSchema[];
  parameters: PcgParameterSchema[];
};

export type PcgGraphNode = {
  id: string;
  type: string;
  config: JsonObject;
  x?: number;
  y?: number;
};

export type PcgGraphEdge = {
  id?: string;
  from: string;
  to: string;
  fromPort?: string;
  toPort?: string;
};

export type PcgGraphDocument = {
  debug?: boolean;
  nodes: PcgGraphNode[];
  edges: PcgGraphEdge[];
  viewport?: {
    x: number;
    y: number;
    zoom: number;
  };
};

export type RuntimeSeverity = 'info' | 'warning' | 'error';

export type RuntimeDiagnostic = {
  severity: RuntimeSeverity;
  message: string;
  nodeId?: string;
  edgeId?: string;
};

export type RuntimeNodeTrace = {
  nodeId: string;
  type: string;
  order: number;
  outputType: PcgDataType;
  inputCount: number;
  inputs: string[];
  outputs: string[];
};

export type RuntimeCompilation = {
  ok: boolean;
  diagnostics: RuntimeDiagnostic[];
  order: string[];
  traces: RuntimeNodeTrace[];
  graph: PcgGraphDocument;
};
