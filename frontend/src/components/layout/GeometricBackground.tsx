export function GeometricBackground() {
  return (
    <div
      className="fixed inset-0 pointer-events-none"
      style={{
        backgroundImage: `
          linear-gradient(rgba(32, 239, 164, 0.03) 1px, transparent 1px),
          linear-gradient(90deg, rgba(32, 239, 164, 0.03) 1px, transparent 1px)
        `,
        backgroundSize: '40px 40px',
      }}
      aria-hidden="true"
    />
  )
}
