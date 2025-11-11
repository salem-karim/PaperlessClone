// App.tsx
import { useEffect, useState } from "react";
import { getDocuments, deleteDocument } from "./lib/documentService";
import { tryCatch } from "./lib/utils";
import type { DocumentSummaryDto } from "./lib/types";
import { Link } from "react-router-dom";
import { FaPlus } from "react-icons/fa6";
import { DocumentList } from "./components/DocumentList";
import { DeleteModal } from "./components/DeleteModal";

export default function App() {
  const [documents, setDocuments] = useState<DocumentSummaryDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [deleteDocId, setDeleteDocId] = useState<string | null>(null);

  async function loadDocuments() {
    setLoading(true);
    const [docs, docsError] =
      await tryCatch<DocumentSummaryDto[]>(getDocuments());
    setLoading(false);

    if (docsError) console.error(docsError.message);
    else if (docs) setDocuments(docs);
  }

  async function handleDelete(id: string) {
    setDeleteDocId(null);
    const [, error] = await tryCatch(deleteDocument(id));
    if (error) console.error(error.message);
    else loadDocuments();
  }

  useEffect(() => {
    loadDocuments();
  }, []);

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <h1 className="text-3xl font-bold mb-4 flex items-center gap-2">
        Documents
        <Link
          to="/documents/new"
          className="hover:bg-blue-100 dark:hover:bg-blue-600 rounded-full p-1 flex items-center justify-center"
        >
          <FaPlus className="text-black dark:text-white" />
        </Link>
      </h1>

      {loading && <p className="text-gray-500 mb-4">Loading documents...</p>}

      <DocumentList documents={documents} onDelete={setDeleteDocId} />

      {deleteDocId && (
        <DeleteModal
          onConfirm={() => handleDelete(deleteDocId)}
          onCancel={() => setDeleteDocId(null)}
        />
      )}
    </div>
  );
}
