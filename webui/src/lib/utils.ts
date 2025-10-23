import type { Result } from "./types";

export async function tryCatch<T, E = Error>(
  prosmise: Promise<T>,
): Promise<Result<T, E>> {
  try {
    const data = await prosmise;
    return [data, null] as const;
  } catch (error) {
    return [null, error as E] as const;
  }
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + " B";
  const kb = bytes / 1024;
  if (kb < 1024) return kb.toFixed(1) + " KB";
  const mb = kb / 1024;
  if (mb < 1024) return mb.toFixed(1) + " MB";
  const gb = mb / 1024;
  return gb.toFixed(2) + " GB";
}

export function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}
