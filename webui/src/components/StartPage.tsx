import { useEffect, useState } from "react";
import { FaTrash, FaPencilAlt } from "react-icons/fa";
import { getDocuments, deleteDocument } from "../lib/documentService";
import { tryCatch } from "../lib/try-catch";
import type { DocumentDto } from "../lib/types";
import { Link } from "react-router-dom";
import { FaPlus } from "react-icons/fa6";

export default function StartPage() {
  const [documents, setDocuments] = useState<DocumentDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [deleteDocId, setDeleteDocId] = useState<string | null>(null);

  async function loadDocuments() {
    setLoading(true);
    const [docs, docsError] = await tryCatch<DocumentDto[]>(getDocuments());
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
          className="text-white hover:bg-blue-600 rounded-full p-1 flex items-center justify-center"
        >
          <FaPlus className="text-white" />
        </Link>
      </h1>

      {loading && <p className="text-gray-500 mb-4">Loading documents...</p>}

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {documents.map((doc) => (
          <div
            key={doc.id}
            className="p-4 border border-gray-300 dark:border-gray-600 rounded-lg shadow-sm transform transition-transform hover:scale-105 hover:bg-gray-100 dark:hover:bg-gray-800"
          >
            <div className="flex justify-between items-center gap-1">
              {/* Clickable title */}
              <Link
                to={`/documents/${doc.id}`}
                className="text-lg font-semibold flex-1 hover:underline"
              >
                {doc.title}
              </Link>

              {/* Action icons */}
              <div className="flex gap-2">
                <Link
                  to={`/documents/${doc.id}/edit`}
                  state={{ from: "/" }} // remember user came from Home
                  className="text-gray-600 dark:text-gray-300 hover:text-blue-500 transition"
                >
                  <FaPencilAlt />
                </Link>

                <button
                  onClick={() => setDeleteDocId(doc.id)}
                  className="text-gray-600 dark:text-gray-300 hover:text-red-500 transition"
                >
                  <FaTrash />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Delete confirmation modal */}
      {deleteDocId && (
        <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 z-50">
          <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-lg max-w-sm w-full text-center">
            <p className="mb-4">
              Are you sure you want to delete this document?
            </p>
            <div className="flex justify-center gap-4">
              <button
                onClick={() => handleDelete(deleteDocId)}
                className="px-4 py-2 bg-red-500 text-white rounded-md hover:bg-red-600 transition"
              >
                Delete
              </button>
              <button
                onClick={() => setDeleteDocId(null)}
                className="px-4 py-2 bg-gray-300 dark:bg-gray-700 rounded-md hover:bg-gray-400 dark:hover:bg-gray-600 transition"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
