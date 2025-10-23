import type {
  DocumentDetailDto,
  DocumentDto,
  DocumentSummaryDto,
} from "./types";

// GET all documents
export async function getDocuments(): Promise<DocumentSummaryDto[]> {
  const res = await fetch("/api/v1/documents");
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json() as Promise<DocumentSummaryDto[]>;
}

// GET document by ID
export async function getDocumentById(id: string): Promise<DocumentDetailDto> {
  const res = await fetch(`/api/v1/documents/${id}`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json() as Promise<DocumentDetailDto>;
}

// POST (create) document
export async function createDocument(
  doc: DocumentDto,
): Promise<DocumentSummaryDto> {
  const form = new FormData();
  form.append("file", doc.file);
  form.append("title", doc.title);
  form.append("createdAt", String(doc.file.lastModified));

  const res = await fetch("/api/v1/documents", {
    method: "POST",
    body: form,
  });

  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json() as Promise<DocumentSummaryDto>;
}

// PUT (update) document Title by ID
export async function updateDocument(
  id: string,
  updates: Pick<DocumentSummaryDto, "title">,
): Promise<DocumentSummaryDto> {
  const res = await fetch(`/api/v1/documents/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(updates),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json() as Promise<DocumentSummaryDto>;
}

// DELETE document by ID
export async function deleteDocument(id: string): Promise<void> {
  const res = await fetch(`/api/v1/documents/${id}`, {
    method: "DELETE",
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
}

// GET OCR status
export async function getOcrStatus(id: string): Promise<{
  id: string;
  ocrStatus: string;
  ocrError?: string;
}> {
  const res = await fetch(`/api/v1/documents/${id}/status`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}
