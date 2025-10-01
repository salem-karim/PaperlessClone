import type { DocumentDto } from "./types";

// GET all documents
export async function getDocuments(): Promise<DocumentDto[]> {
  const res = await fetch("/api/v1/documents");
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json() as Promise<DocumentDto[]>;
}

// GET document by ID
export async function getDocumentById(id: string): Promise<DocumentDto> {
  const res = await fetch(`/api/v1/documents/${id}`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json() as Promise<DocumentDto>;
}

// POST (create) document
export async function createDocument(
  doc: Omit<DocumentDto, "id">,
): Promise<DocumentDto> {
  const res = await fetch("/api/v1/documents", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(doc),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json() as Promise<DocumentDto>;
}

// PUT (update) document by ID
export async function updateDocument(
  id: string,
  doc: Partial<DocumentDto>,
): Promise<DocumentDto> {
  const res = await fetch(`/api/v1/documents/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(doc),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json() as Promise<DocumentDto>;
}

// DELETE document by ID
export async function deleteDocument(id: string): Promise<void> {
  const res = await fetch(`/api/v1/documents/${id}`, {
    method: "DELETE",
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
}
