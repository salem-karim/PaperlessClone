import { useEffect, useState } from "react";
import { getDocuments } from "../lib/documentService";
import { tryCatch } from "../lib/try-catch";
import type { DocumentDto } from "../lib/types";

export default function StartPage() {
  const [documents, setDocuments] = useState<DocumentDto[]>([]);
  const [loading, setLoading] = useState(false);

  async function loadDocuments() {
    setLoading(true);
    const [docs, docsError] = await tryCatch<DocumentDto[]>(getDocuments());
    setLoading(false);

    if (docsError) {
      console.error(docsError.message);
    } else if (docs) {
      setDocuments(docs);
    }
  }

  useEffect(() => {
    loadDocuments();
  }, []);

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <header className="flex gap-4 items-center mb-6"></header>

      <h1 className="text-3xl font-bold mb-4">Documents</h1>

      {loading && <p className="text-gray-500 mb-4">Loading documents...</p>}

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {documents.map((doc) => (
          <div
            key={doc.id}
            className="p-4 border border-gray-300 rounded-lg shadow-sm cursor-pointer transform transition-transform hover:scale-105"
            onClick={() => alert(`Clicked document: ${doc.title}`)}
          >
            <h2 className="text-lg font-semibold">{doc.title}</h2>
          </div>
        ))}
      </div>
    </div>
  );
}
