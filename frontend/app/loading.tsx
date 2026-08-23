export default function Loading() {
  return (
    <main>
      <div className="loading-header">
        <div className="skeleton skeleton-title" />
        <div className="skeleton skeleton-subtitle" />
      </div>

      <div className="loading-grid">
        <div className="skeleton skeleton-card" />
        <div className="skeleton skeleton-card" />
      </div>
    </main>
  );
}