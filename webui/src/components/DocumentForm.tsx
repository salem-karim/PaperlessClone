import { useState, useEffect, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { createDocument } from "../lib/documentService";
import { getCategories } from "../lib/categoryService";
import { tryCatch, getContrastColor } from "../lib/utils";
import type { CategoryDto } from "../lib/types";
import { CategoryIcon } from "../lib/iconMapping";

export default function DocumentForm() {
  const [title, setTitle] = useState("");
  const [error, setError] = useState("");
  const [file, setFile] = useState<File>();
  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [selectedCategories, setSelectedCategories] = useState<CategoryDto[]>([]);
  const navigate = useNavigate();

  const SUPPORTED_MIME_TYPES = [
    "application/pdf",
    "image/png",
    "image/jpeg",
    "image/jpg",
    "image/tiff",
    "image/bmp",
    "image/gif",
  ];

  // Load categories on mount
  useEffect(() => {
    loadCategories();
  }, []);

  async function loadCategories() {
    const [cats, err] = await tryCatch(getCategories());
    if (err) console.error(err);
    else if (cats) setCategories(cats);
  }

  function toggleCategory(cat: CategoryDto) {
    const isSelected = selectedCategories.some((c) => c.id === cat.id);
    if (isSelected) {
      setSelectedCategories(selectedCategories.filter((c) => c.id !== cat.id));
    } else {
      setSelectedCategories([...selectedCategories, cat]);
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!title.trim() || !file) {
      setError("Please provide a title and select a file (PDF or image).");
      return;
    }
    if (!SUPPORTED_MIME_TYPES.includes(file.type)) {
      setError("Unsupported file type. Please upload a PDF, PNG, JPG, TIFF, BMP, or GIF file.");
      return;
    }
    setLoading(true);
    const [, err] = await tryCatch(createDocument({ title, file, categories: selectedCategories }));
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
          <label className="font-semibold">File (PDF or Image)</label>
          <input
            type="file"
            accept="application/pdf,image/png,image/jpeg,image/jpg,image/tiff,image/bmp,image/gif"
            onChange={(e) => setFile(e.target.files?.[0])}
            className="cursor-pointer px-3 py-2 border rounded-md text-gray-700 dark:text-white bg-white dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600 transition"
          />
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Supported formats: PDF, PNG, JPG, JPEG, TIFF, BMP, GIF
          </p>
        </div>

        {/* Categories Selection */}
        <div className="flex flex-col gap-2">
          <label className="font-semibold">Categories (Optional)</label>
          {categories.length === 0 ? (
            <p className="text-sm text-gray-500 dark:text-gray-400">
              No categories available.{" "}
              <button
                type="button"
                onClick={() => navigate("/categories")}
                className="text-blue-500 hover:underline"
              >
                Create one
              </button>
            </p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {categories.map((cat) => {
                const isSelected = selectedCategories.some((c) => c.id === cat.id);
                return (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => toggleCategory(cat)}
                    className={`px-3 py-1 rounded-full text-sm font-medium transition flex items-center gap-1 ${
                      isSelected
                        ? "ring-2 ring-offset-2 ring-blue-500 dark:ring-offset-gray-800"
                        : "opacity-70 hover:opacity-100 hover:scale-105"
                    }`}
                    style={{
                      backgroundColor: cat.color,
                      color: getContrastColor(cat.color),
                    }}
                  >
                    <CategoryIcon iconName={cat.icon} />
                    {cat.name}
                  </button>
                );
              })}
            </div>
          )}
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
