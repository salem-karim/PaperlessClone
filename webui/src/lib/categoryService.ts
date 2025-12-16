import type { CategoryDto } from "./types";

// GET all categories
export async function getCategories(): Promise<CategoryDto[]> {
  const res = await fetch("/api/v1/categories");
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json() as Promise<CategoryDto[]>;
}

// GET category by ID
export async function getCategoryById(id: string): Promise<CategoryDto> {
  const res = await fetch(`/api/v1/categories/${id}`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json() as Promise<CategoryDto>;
}

// POST (create) category
export async function createCategory(
  category: Omit<CategoryDto, "id" | "createdAt" | "updatedAt">,
): Promise<CategoryDto> {
  const res = await fetch("/api/v1/categories", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(category),
  });

  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json() as Promise<CategoryDto>;
}

// PUT (update) category
export async function updateCategory(
  category: CategoryDto,
): Promise<CategoryDto> {
  const res = await fetch(`/api/v1/categories`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(category),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json() as Promise<CategoryDto>;
}

// DELETE category by ID
export async function deleteCategory(id: string): Promise<void> {
  const res = await fetch(`/api/v1/categories/${id}`, {
    method: "DELETE",
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
}
