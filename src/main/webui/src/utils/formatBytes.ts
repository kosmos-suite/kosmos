/** Bytes to a TB figure with one decimal place, e.g. 3000000000000 -> "2.7 TB". */
export function formatTb(bytes: number): string {
  return `${(bytes / 1024 ** 4).toFixed(1)} TB`;
}
