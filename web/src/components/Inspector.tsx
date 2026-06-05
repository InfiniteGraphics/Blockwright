import type { PcgFlowNode } from '../runtime/graphCodec';
import type { JsonValue, PcgParameterSchema } from '../runtime/types';

type Props = {
  node: PcgFlowNode | undefined;
  onConfigChange(nodeId: string, key: string, value: JsonValue): void;
  onDelete(nodeId: string): void;
};

export function Inspector({ node, onConfigChange, onDelete }: Props) {
  if (!node) {
    return (
      <aside className="panel inspector empty">
        <header className="panel-header">
          <div>
            <h2>Inspector</h2>
            <p>Select a node to edit parameters.</p>
          </div>
        </header>
      </aside>
    );
  }

  const schema = node.data.schema;
  const params = schema?.parameters ?? [];

  return (
    <aside className="panel inspector">
      <header className="panel-header split">
        <div>
          <h2>{node.data.label}</h2>
          <p>{node.id}</p>
        </div>
        <button className="danger" type="button" onClick={() => onDelete(node.id)}>Delete</button>
      </header>
      <div className="inspector__meta">
        <span>{node.data.category}</span>
        <span>{node.data.pcgType}</span>
      </div>
      <p className="inspector__description">{node.data.description ?? 'No description.'}</p>
      <div className="inspector__params">
        {params.length === 0 && <p className="muted">This node has no editable parameters.</p>}
        {params.map((parameter) => (
          <ParameterInput
            key={parameter.id}
            parameter={parameter}
            value={node.data.config[parameter.id] ?? parameter.default ?? ''}
            onChange={(value) => onConfigChange(node.id, parameter.id, value)}
          />
        ))}
      </div>
    </aside>
  );
}

type ParameterInputProps = {
  parameter: PcgParameterSchema;
  value: JsonValue;
  onChange(value: JsonValue): void;
};

function ParameterInput({ parameter, value, onChange }: ParameterInputProps) {
  const label = parameter.id;
  const description = parameter.description;
  if (parameter.type === 'bool') {
    return (
      <label className="field checkbox-field">
        <input type="checkbox" checked={Boolean(value)} onChange={(event) => onChange(event.target.checked)} />
        <span>
          <b>{label}</b>
          {description && <small>{description}</small>}
        </span>
      </label>
    );
  }
  if (parameter.type === 'int' || parameter.type === 'number') {
    return (
      <label className="field">
        <span>{label}</span>
        <input
          type="number"
          value={typeof value === 'number' ? value : Number(value || 0)}
          step={parameter.type === 'int' ? 1 : 'any'}
          min={parameter.min}
          max={parameter.max}
          onChange={(event) => onChange(parameter.type === 'int' ? Math.trunc(Number(event.target.value)) : Number(event.target.value))}
        />
        {description && <small>{description}</small>}
      </label>
    );
  }
  return (
    <label className="field">
      <span>{label}</span>
      <input type="text" value={typeof value === 'string' ? value : String(value ?? '')} onChange={(event) => onChange(event.target.value)} />
      {description && <small>{description}</small>}
    </label>
  );
}
