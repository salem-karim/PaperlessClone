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

/**
 * Calculate if text should be black or white based on background color
 * Uses relative luminance formula (WCAG)
 */
export function getContrastColor(hexColor: string): string {
  // Remove # if present
  const hex = hexColor.replace("#", "");
  
  // Convert to RGB
  const r = parseInt(hex.substring(0, 2), 16) / 255;
  const g = parseInt(hex.substring(2, 4), 16) / 255;
  const b = parseInt(hex.substring(4, 6), 16) / 255;
  
  // Calculate relative luminance
  const luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
  
  // Return black for light backgrounds, white for dark backgrounds
  return luminance > 0.5 ? "#000000" : "#FFFFFF";
}
