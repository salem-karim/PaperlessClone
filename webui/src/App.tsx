import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { FaPlus } from "react-icons/fa6";

import {
  getDocuments,
  deleteDocument,
  searchDocuments,
} from "./lib/documentService";
import { tryCatch } from "./lib/utils";
import type { DocumentSummaryDto } from "./lib/types";

import { DocumentList } from "./components/DocumentList";
import { DeleteModal } from "./components/DeleteModal";
import { SearchBar } from "./components/SearchBar";

export default function App() {
  const navigate = useNavigate();
  const location = useLocation();

  const urlParams = new URLSearchParams(location.search);
  const initialQuery = urlParams.get("q") ?? "";

  const [query, setQuery] = useState(initialQuery);
  const [documents, setDocuments] = useState<DocumentSummaryDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [deleteDocId, setDeleteDocId] = useState<string | null>(null);
  const [noResults, setNoResults] = useState(false);

  async function loadAll() {
    setLoading(true);
    const [docs, error] = await tryCatch<DocumentSummaryDto[]>(getDocuments());
    setLoading(false);

    if (error) console.error(error.message);
    else if (docs) {
      setDocuments(docs);
      setNoResults(false);
    }
  }

  async function loadSearch(q: string) {
    setLoading(true);
    const [docs, error] = await tryCatch<DocumentSummaryDto[]>(
      searchDocuments(q),
    );
    setLoading(false);

    if (error) {
      console.error(error.message);
      return;
    }

    setDocuments(docs ?? []);
    setNoResults((docs ?? []).length === 0);
  }

  function applySearch(q: string) {
    if (!q.trim()) {
      navigate("/");
      return;
    }
    navigate(`/?q=${encodeURIComponent(q)}`);
  }

  useEffect(() => {
    const urlParams = new URLSearchParams(location.search);
    const query = urlParams.get("q")?.trim() ?? "";

    if (!query) {
      loadAll();
    } else {
      loadSearch(query);
    }
     
  }, [location.search]);

  async function handleDelete(id: string) {
    setDeleteDocId(null);
    const [, error] = await tryCatch(deleteDocument(id));
    if (error) console.error(error.message);
    else {
      const q = urlParams.get("q") ?? "";
      if (q) loadSearch(q);
      else loadAll();
    }
  }

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="flex items-center gap-4 mb-4">
        <h1 className="text-3xl font-bold">Documents</h1>

        <Link
          to="/documents/new"
          className="p-2 hover:bg-blue-100 dark:hover:bg-blue-600 rounded-full flex items-center justify-center transition"
          title="Upload new document"
        >
          <FaPlus className="text-xl text-black dark:text-white" />
        </Link>

        <SearchBar
          value={query}
          onChange={setQuery}
          onSubmit={() => applySearch(query)}
        />
      </div>

      {loading && <p className="text-gray-500 mb-4">Loading documents...</p>}

      {noResults && !loading && (
        <p className="text-gray-600 italic mb-4">
          No documents found for "{query}"
        </p>
      )}

      {!noResults && (
        <DocumentList documents={documents} onDelete={setDeleteDocId} />
      )}

      {deleteDocId && (
        <DeleteModal
          onConfirm={() => handleDelete(deleteDocId)}
          onCancel={() => setDeleteDocId(null)}
        />
      )}
    </div>
  );
}
