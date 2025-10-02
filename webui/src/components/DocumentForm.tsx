import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import {
  createDocument,
  getDocumentById,
  updateDocument,
} from "../lib/documentService";
import { tryCatch } from "../lib/try-catch";

export default function DocumentForm() {
  const { id } = useParams<{ id: string }>();
  const [title, setTitle] = useState("");
  const [loading, setLoading] = useState(false);
  const location = useLocation();
  const from = (location.state as { from?: string })?.from || "/";
  const navigate = useNavigate();
  const isEdit = Boolean(id);

  // Load document if editing
  useEffect(() => {
    if (!id) return;
    setLoading(true);
    tryCatch(getDocumentById(id)).then(([doc, err]) => {
      setLoading(false);
      if (err) console.error(err);
      else if (doc) setTitle(doc.title);
    });
  }, [id]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;

    setLoading(true);
    if (isEdit && id) {
      const [, err] = await tryCatch(updateDocument(id, { title }));
      setLoading(false);
      if (err) console.error(err);
      else navigate(`/documents/${id}`);
    } else {
      const [, err] = await tryCatch(createDocument({ title }));
      setLoading(false);
      if (err) console.error(err);
      else navigate("/");
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="max-w-md mx-auto mt-6 p-4 border rounded-lg shadow-md bg-white dark:bg-gray-800 flex flex-col gap-4"
    >
      <h1 className="text-2xl font-bold">
        {isEdit ? "Edit Document" : "New Document"}
      </h1>
      {loading && <p className="text-gray-500">Loading...</p>}

      <label className="block font-semibold">Title</label>
      <input
        type="text"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
      />

      <div className="flex gap-4">
        <button
          type="submit"
          className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition"
        >
          Save
        </button>
        <button
          type="button"
          onClick={() => navigate(from)}
          className="px-4 py-2 bg-gray-300 dark:bg-gray-700 rounded-md hover:bg-gray-400 dark:hover:bg-gray-600 transition"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
