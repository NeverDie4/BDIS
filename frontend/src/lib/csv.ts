export function escapeCsvCell(value: string | number | undefined) {
  let text = value == null ? "" : String(value);
  if (typeof value === "string" && /^\s*[=+\-@]/.test(value)) {
    text = `'${value}`;
  }
  return `"${text.replaceAll('"', '""')}"`;
}
