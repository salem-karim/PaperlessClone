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
  
  // Add category IDs if provided
  if (doc.categories && doc.categories.length > 0) {
    doc.categories.forEach(cat => {
      if (cat.id) {
        form.append("categoryIds", cat.id);
      }
    });
  }

  const res = await fetch("/api/v1/documents", {
    method: "POST",
    body: form,
  });

  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json() as Promise<DocumentSummaryDto>;
}

// PUT (update) document Title and Categories by ID
export async function updateDocument(
  id: string,
  updates: Partial<Pick<DocumentSummaryDto, "title" | "categories">>,
): Promise<DocumentSummaryDto> {
  // First fetch the current document to get all fields
  const currentDoc = await getDocumentById(id);
  
  // Build the full DocumentSummaryDto with updates
  const fullUpdate: DocumentSummaryDto = {
    id: currentDoc.id,
    title: updates.title !== undefined ? updates.title : currentDoc.title,
    originalFilename: currentDoc.originalFilename,
    fileSize: currentDoc.fileSize,
    contentType: currentDoc.contentType,
    processingStatus: currentDoc.processingStatus,
    createdAt: currentDoc.createdAt,
    categories: updates.categories !== undefined ? updates.categories : currentDoc.categories,
  };
  
  const res = await fetch(`/api/v1/documents/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(fullUpdate),
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

export async function searchDocuments(query: string, categories?: string[]): Promise<DocumentSummaryDto[]> {
  // Ensure query has at least empty string
  const searchQuery = query || "";
  let url = `/api/v1/documents/search?q=${encodeURIComponent(searchQuery)}`;
  
  if (categories && categories.length > 0) {
    const categoryParams = categories.map(c => `categories=${encodeURIComponent(c)}`).join('&');
    url += `&${categoryParams}`;
  }
  
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Search failed: HTTP ${res.status}`);
  return res.json() as Promise<DocumentSummaryDto[]>;
}
