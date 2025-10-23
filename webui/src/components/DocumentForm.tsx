import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { createDocument } from "../lib/documentService";
import { tryCatch } from "../lib/utils";

export default function DocumentForm() {
  const [title, setTitle] = useState("");
  const [error, setError] = useState("");
  const [file, setFile] = useState<File>();
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!title.trim() || !file) {
      setError("Please provide a title and select a PDF file.");
      return;
    }
    if (file.type !== "application/pdf") {
      setError("Only PDF files are allowed.");
      return;
    }
    setLoading(true);
    const [, err] = await tryCatch(createDocument({ title, file }));
    setLoading(false);
    if (err) setError("Failed to upload document.");
    else navigate("/");
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-2xl p-8 border rounded-lg shadow-md bg-white dark:bg-gray-800 flex flex-col gap-6"
      >
        <h1 className="text-3xl font-bold">New Document</h1>
        {loading && <p className="text-gray-500">Loading...</p>}

        <div className="flex flex-col gap-2">
          <label className="block font-semibold text-lg">Title</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full px-4 py-3 text-lg border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
            placeholder="Enter document title..."
          />
        </div>

        <div className="flex flex-col gap-2">
          <label className="font-semibold">File</label>
          <input
            type="file"
            accept="application/pdf"
            onChange={(e) => setFile(e.target.files?.[0])}
            className="cursor-pointer px-3 py-2 border rounded-md text-gray-700 dark:text-white bg-white dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600 transition"
          />
        </div>
        {error && <p className="text-red-500">{error}</p>}

        <div className="flex gap-4 mt-4 justify-end-safe">
          <button
            type="submit"
            disabled={loading}
            className="px-6 py-3 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? "Saving..." : "Save"}
          </button>
          <button
            type="button"
            onClick={() => navigate("/")}
            className="px-6 py-3 bg-gray-300 dark:bg-gray-700 rounded-md hover:bg-gray-400 dark:hover:bg-gray-600 transition"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
