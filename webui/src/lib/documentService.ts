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

// GET processing status
export async function getProcessingStatus(id: string): Promise<{
  id: string;
  processingStatus: string;
  processingError?: string;
}> {
  const res = await fetch(`/api/v1/documents/${id}/status`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

// Download document (handles both streaming and presigned URLs)
export async function downloadDocument(
  id: string,
  filename: string,
): Promise<void> {
  const res = await fetch(`/api/v1/documents/${id}/download`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);

  const contentType = res.headers.get("content-type");

  // Check if response is JSON (presigned URL) or binary (direct stream)
  if (contentType && contentType.includes("application/json")) {
    // Large file: response contains presigned URL
    const data = await res.json();

    // Open the presigned URL in a new tab to trigger download
    const link = document.createElement("a");
    link.href = data.url;
    link.download = filename;
    link.target = "_blank";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } else {
    // Small file: response is the file itself
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }
}
