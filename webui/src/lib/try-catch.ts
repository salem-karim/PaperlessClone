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
