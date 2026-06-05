import type { PcgNodeSchema } from '../runtime/types';

type Props = {
  schemas: PcgNodeSchema[];
  onAdd(type: string): void;
};

export function NodeLibrary({ schemas, onAdd }: Props) {
  const groups = schemas.reduce<Map<string, PcgNodeSchema[]>>((map, schema) => {
    const category = schema.category || 'Other';
    const list = map.get(category) ?? [];
    list.push(schema);
    map.set(category, list);
    return map;
  }, new Map());

  return (
    <aside className="panel node-library">
      <header className="panel-header">
        <div>
          <h2>PCG Nodes</h2>
          <p>Java node schema driven</p>
        </div>
      </header>
      <div className="node-library__groups">
        {[...groups.entries()].map(([category, items]) => (
          <section key={category}>
            <h3>{category}</h3>
            {items.map((schema) => (
              <button className="node-card" key={schema.type} type="button" onClick={() => onAdd(schema.type)}>
                <span>{schema.displayName}</span>
                <small>{schema.description ?? schema.type}</small>
              </button>
            ))}
          </section>
        ))}
      </div>
    </aside>
  );
}
