const TONES: [string, string, string][] = [
  ["#232741", "#5b4a7a", "#16222e"],
  ["#1e2438", "#3e5470", "#16262e"],
  ["#31283c", "#7a4e54", "#1c1f2e"],
  ["#1d2333", "#3b4a66", "#141b26"],
  ["#2b2438", "#5e4262", "#181d2b"],
  ["#1f2a2c", "#4a6e58", "#141f26"],
];

/** Deterministic placeholder gradient for a poster-shaped thumbnail, keyed by an arbitrary index. */
export function tonalGradient(index: number): string {
  const [a, b, c] = TONES[((index % TONES.length) + TONES.length) % TONES.length];
  const deg = 140 + ((index * 13) % 40);
  return `linear-gradient(${deg}deg, ${a}, ${b} 50%, ${c})`;
}
