import type { BridgePreview } from '../bridge/blockwrightBridge';
import type { RuntimeCompilation } from '../runtime/types';

type Props = {
  compilation: RuntimeCompilation | undefined;
  preview: BridgePreview | undefined;
  status: string;
};

export function RuntimePanel({ compilation, preview, status }: Props) {
  return (
    <aside className="panel runtime-panel">
      <header className="panel-header">
        <div>
          <h2>Runtime</h2>
          <p>{status || 'Ready'}</p>
        </div>
      </header>
      <section className="runtime-card">
        <h3>Compile</h3>
        <div className={`runtime-badge ${compilation?.ok ? 'ok' : 'warn'}`}>{compilation?.ok ? 'Valid DAG' : 'Needs attention'}</div>
        <p>{compilation?.order.length ?? 0} nodes in execution order</p>
      </section>
      <section className="runtime-card">
        <h3>MC Preview</h3>
        <p>{preview?.available ? `${preview.blockCount ?? 0} planned blocks` : 'No preview yet'}</p>
        {preview?.severity && <div className="runtime-badge">{preview.severity}</div>}
      </section>
      <section className="runtime-card">
        <h3>Node Outputs</h3>
        {(!preview?.nodeSummaries || preview.nodeSummaries.length === 0) && <p className="muted">Run preview to inspect node output.</p>}
        {preview?.nodeSummaries?.slice().sort((a, b) => a.order - b.order).map((summary) => (
          <div className="node-summary" key={`${summary.order}-${summary.nodeId}`}>
            <div>
              <b>{summary.nodeId}</b>
              <span>{summary.type}</span>
            </div>
            <dl>
              <dt>in</dt>
              <dd>{summary.inputCount}</dd>
              <dt>pts</dt>
              <dd>{summary.pointCount}</dd>
              <dt>vol</dt>
              <dd>{summary.volumeCount}</dd>
              <dt>blk</dt>
              <dd>{summary.plannedBlockDelta}</dd>
            </dl>
          </div>
        ))}
      </section>
      <section className="diagnostics">
        <h3>Diagnostics</h3>
        {(!compilation || compilation.diagnostics.length === 0) && <p className="muted">No runtime diagnostics.</p>}
        {compilation?.diagnostics.map((diagnostic, index) => (
          <div className={`diagnostic ${diagnostic.severity}`} key={`${diagnostic.message}-${index}`}>
            <b>{diagnostic.severity}</b>
            <span>{diagnostic.message}</span>
          </div>
        ))}
        {preview?.issues?.map((issue, index) => (
          <div className={`diagnostic ${issue.severity.toLowerCase()}`} key={`${issue.message}-${index}`}>
            <b>{issue.severity}</b>
            <span>{issue.message}</span>
          </div>
        ))}
      </section>
    </aside>
  );
}
