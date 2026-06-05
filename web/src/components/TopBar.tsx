import type { EditorSession } from '../bridge/blockwrightBridge';

type Props = {
  session: EditorSession | undefined;
  dirty: boolean;
  busy: boolean;
  onSelect(packId: string, presetId: string): void;
  onReload(): void;
  onPreview(): void;
  onSave(): void;
  onBake(): void;
  onExport(): void;
  onUndo(): void;
  onRedo(): void;
  canUndo: boolean;
  canRedo: boolean;
};

export function TopBar({ session, dirty, busy, onSelect, onReload, onPreview, onSave, onBake, onExport, onUndo, onRedo, canUndo, canRedo }: Props) {
  const selected = session ? `${session.packId}::${session.presetId}` : '';
  return (
    <header className="topbar">
      <div className="brand">
        <div className="brand-mark">BW</div>
        <div>
          <h1>Blockwright PCG</h1>
          <p>React Flow editor · TS runtime · MC authoritative preview</p>
        </div>
      </div>
      <select
        value={selected}
        disabled={!session || busy}
        onChange={(event) => {
          const [packId, presetId] = event.target.value.split('::');
          onSelect(packId, presetId);
        }}
      >
        {session?.packs.flatMap((pack) => pack.presets.map((preset) => (
          <option key={`${pack.id}::${preset.id}`} value={`${pack.id}::${preset.id}`}>
            {pack.name} / {preset.name}
          </option>
        )))}
      </select>
      <div className="topbar-actions">
        {dirty && <span className="dirty">Unsaved</span>}
        <button type="button" onClick={onUndo} disabled={busy || !canUndo} title="Undo graph edit">Undo</button>
        <button type="button" onClick={onRedo} disabled={busy || !canRedo} title="Redo graph edit">Redo</button>
        <button type="button" onClick={onReload} disabled={busy}>Reload</button>
        <button type="button" onClick={onPreview} disabled={busy}>Preview</button>
        <button type="button" onClick={onSave} disabled={busy || !session?.devSaveAvailable}>Save</button>
        <button type="button" onClick={onBake} disabled={busy}>Bake</button>
        <button type="button" onClick={onExport} disabled={busy}>Export JSON</button>
      </div>
    </header>
  );
}
