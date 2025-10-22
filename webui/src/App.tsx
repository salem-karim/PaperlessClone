// App.tsx
import { useEffect, useState } from "react";
import { getDocuments, deleteDocument } from "./lib/documentService";
import { tryCatch } from "./lib/utils";
import type { DocumentSummaryDto } from "./lib/types";
import { Link } from "react-router-dom";
import { FaPlus } from "react-icons/fa6";
import { DocumentList } from "./components/DocumentList";
import { DeleteModal } from "./components/DeleteModal";

const MOCK_DOCUMENTS: DocumentSummaryDto[] = [
  {
    id: "1",
    title: "Annual Report 2024",
    originalFileName: "annual_report_2024.pdf",
    fileSize: 2456789, // bytes
    createdAt: new Date().toISOString(),
  },
  {
    id: "2",
    title: "Meeting Notes",
    originalFileName: "meeting_notes.docx",
    fileSize: 123456,
    createdAt: new Date(Date.now() - 86400000).toISOString(), // yesterday
  },
  {
    id: "3",
    title: "Project Proposal",
    originalFileName: "proposal.pdf",
    fileSize: 987654,
    createdAt: new Date(Date.now() - 3 * 86400000).toISOString(), // 3 days ago
  },
  {
    id: "4",
    title: "Budget Plan",
    originalFileName: "budget.xlsx",
    fileSize: 567890,
    createdAt: new Date(Date.now() - 7 * 86400000).toISOString(), // 1 week ago
  },
];

export default function App() {
  const [documents, setDocuments] = useState<DocumentSummaryDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [deleteDocId, setDeleteDocId] = useState<string | null>(null);

  async function loadDocuments() {
    setLoading(true);
    // comment out real API call during UI dev
    // const [docs, docsError] = await tryCatch<DocumentSummaryDto[]>(getDocuments());
    // setLoading(false);
    // if (docsError) console.error(docsError.message);
    // else if (docs) setDocuments(docs);

    // use mock data
    setTimeout(() => {
      setDocuments(MOCK_DOCUMENTS);
      setLoading(false);
    }, 500); // simulate network delay
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
