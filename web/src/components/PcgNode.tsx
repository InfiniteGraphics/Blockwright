import { Handle, Position, type NodeProps } from '@xyflow/react';
import type { PcgFlowNode } from '../runtime/graphCodec';

export function PcgNode({ data, selected }: NodeProps<PcgFlowNode>) {
  const inputs = data.inputs ?? [];
  const outputs = data.outputs?.length ? data.outputs : [{ id: 'output', dataType: data.outputType }];
  return (
    <div className={`pcg-node ${selected ? 'is-selected' : ''}`}>
      <div className="pcg-node__category">{data.category}</div>
      <div className="pcg-node__title">{data.label}</div>
      <div className="pcg-node__type">{data.pcgType}</div>
      <div className="pcg-node__ports">
        <div className="pcg-node__port-list">
          {inputs.map((port, index) => (
            <div className="pcg-node__port input" key={port.id}>
              <Handle
                type="target"
                position={Position.Left}
                id={port.id}
                className="pcg-handle input"
                style={{ top: portOffset(index, inputs.length) }}
              />
              <span>{port.label ?? port.id}</span>
              <b>{port.dataType}</b>
            </div>
          ))}
        </div>
        <div className="pcg-node__port-list outputs">
          {outputs.map((port, index) => (
            <div className="pcg-node__port output" key={port.id}>
              <span>{port.label ?? port.id}</span>
              <b>{port.dataType}</b>
              <Handle
                type="source"
                position={Position.Right}
                id={port.id}
                className="pcg-handle output"
                style={{ top: portOffset(index, outputs.length) }}
              />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function portOffset(index: number, total: number): string {
  if (total <= 1) {
    return '50%';
  }
  return `${30 + (index * 40) / Math.max(1, total - 1)}%`;
}
