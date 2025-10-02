import { useEffect, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { getDocumentById } from "../lib/documentService";
import { tryCatch } from "../lib/try-catch";
import type { DocumentDto } from "../lib/types";
import { FaHouse, FaPencil } from "react-icons/fa6";

export default function DocumentDetails() {
  const { id } = useParams<{ id: string }>();
  const [document, setDocument] = useState<DocumentDto | null>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    tryCatch(getDocumentById(id)).then(([doc, err]) => {
      setLoading(false);
      if (err) console.error(err);
      else setDocument(doc || null);
    });
  }, [id]);

  if (loading)
    return <p className="text-gray-500 mt-6 text-center">Loading...</p>;
  if (!document)
    return <p className="text-red-500 mt-6 text-center">Document not found.</p>;

  return (
    <div className="max-w-md mx-auto mt-6 p-4 border rounded-lg shadow-md bg-white dark:bg-gray-800 flex flex-col gap-4">
      <h1 className="text-2xl font-bold">{document.title}</h1>
      <p className="text-gray-600 dark:text-gray-300">ID: {document.id}</p>
      <div className="flex gap-4">
        <Link
          to={`/documents/${document.id}/edit`}
          state={{ from: `/documents/${document.id}` }} // pass Details page as "from"
          className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition"
        >
          <FaPencil />
        </Link>
        <button
          onClick={() => navigate("/")}
          className="px-4 py-2 bg-gray-300 dark:bg-gray-700 rounded-md hover:bg-gray-400 dark:hover:bg-gray-600 transition"
        >
          <FaHouse />
        </button>
      </div>
    </div>
  );
}
