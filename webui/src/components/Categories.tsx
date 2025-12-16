import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { FaPlus, FaEdit, FaTrash, FaSave, FaTimes } from "react-icons/fa";
import { FaHouse } from "react-icons/fa6";
import type { CategoryDto } from "../lib/types";
import {
  getCategories,
  createCategory,
  updateCategory,
  deleteCategory,
} from "../lib/categoryService";
import { tryCatch, getContrastColor } from "../lib/utils";
import { CategoryIcon } from "../lib/iconMapping";
import { iconOptions, type IconName } from "../lib/iconConstants";
import { DeleteModal } from "./DeleteModal";

export default function Categories() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [categoryToDelete, setCategoryToDelete] = useState<string | null>(null);

  // Form state for editing/creating
  const [formName, setFormName] = useState("");
  const [formColor, setFormColor] = useState("#3B82F6");
  const [formIcon, setFormIcon] = useState<IconName>("tag");
  const [error, setError] = useState("");

  useEffect(() => {
    loadCategories();
  }, []);

  async function loadCategories() {
    setLoading(true);
    const [cats, err] = await tryCatch(getCategories());
    setLoading(false);
    if (err) console.error(err);
    else if (cats) setCategories(cats);
  }

  function startCreate() {
    setIsCreating(true);
    setFormName("");
    setFormColor("#3B82F6");
    setFormIcon("tag");
    setError("");
  }

  function cancelCreate() {
    setIsCreating(false);
    setFormName("");
    setFormColor("#3B82F6");
    setFormIcon("tag");
    setError("");
  }

  async function handleCreate() {
    if (!formName.trim()) {
      setError("Name is required");
      return;
    }

    if (formName.trim().length > 50) {
      setError("Category name must not exceed 50 characters");
      return;
    }

    setLoading(true);
    const [newCat, err] = await tryCatch(
      createCategory({
        name: formName.trim(),
        color: formColor,
        icon: formIcon,
      }),
    );
    setLoading(false);

    if (err) {
      setError("Failed to create category. Name might already exist.");
      return;
    }

    if (newCat) {
      setCategories([...categories, newCat]);
      cancelCreate();
    }
  }

  function startEdit(cat: CategoryDto) {
    setEditingId(cat.id || null);
    setFormName(cat.name);
    setFormColor(cat.color);
    setFormIcon(cat.icon as IconName);
    setError("");
  }

  function cancelEdit() {
    setEditingId(null);
    setFormName("");
    setFormColor("#3B82F6");
    setFormIcon("tag");
    setError("");
  }

  async function handleUpdate() {
    if (!editingId) return;
    if (!formName.trim()) {
      setError("Name is required");
      return;
    }

    if (formName.trim().length > 50) {
      setError("Category name must not exceed 50 characters");
      return;
    }

    const categoryToUpdate = categories.find((c) => c.id === editingId);
    if (!categoryToUpdate) return;

    setLoading(true);
    const [updated, err] = await tryCatch(
      updateCategory({
        ...categoryToUpdate,
        name: formName.trim(),
        color: formColor,
        icon: formIcon,
      }),
    );
    setLoading(false);

    if (err) {
      setError("Failed to update category. Name might already be in use.");
      return;
    }

    if (updated) {
      setCategories(categories.map((c) => (c.id === updated.id ? updated : c)));
      cancelEdit();
    }
  }

  function handleDelete(id: string) {
    setCategoryToDelete(id);
  }

  async function confirmDelete() {
    if (!categoryToDelete) return;

    setLoading(true);
    const [, err] = await tryCatch(deleteCategory(categoryToDelete));
    setLoading(false);

    if (err) {
      console.error("Delete error:", err);
      setError(
        `Failed to delete category: ${err instanceof Error ? err.message : String(err)}`,
      );
      setCategoryToDelete(null);
      return;
    }

    setCategories(categories.filter((c) => c.id !== categoryToDelete));
    setCategoryToDelete(null);
  }

  return (
    <div className="p-4 sm:p-6 max-w-6xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
        <h1 className="text-2xl sm:text-3xl font-bold">Categories / Tags</h1>
        <button
          onClick={() => navigate("/")}
          className="p-2 bg-gray-300 dark:bg-gray-700 rounded-md hover:bg-gray-400 dark:hover:bg-gray-600 transition"
          title="Back to Documents"
        >
          <FaHouse />
        </button>
      </div>

      {loading && <p className="text-gray-500 mb-4">Loading...</p>}

      {error && (
        <p className="text-red-500 mb-4 p-3 bg-red-100 dark:bg-red-900/30 rounded">
          {error}
        </p>
      )}

      {/* Create New Category Button */}
      {!isCreating && (
        <button
          onClick={startCreate}
          className="mb-4 px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition flex items-center gap-2"
        >
          <FaPlus /> Add New Category
        </button>
      )}

      {/* Create Form */}
      {isCreating && (
        <div className="mb-6 p-3 sm:p-4 border border-blue-500 rounded-lg bg-blue-50 dark:bg-blue-900/20">
          <h2 className="text-lg sm:text-xl font-semibold mb-3">
            Create New Category
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-3">
            <div>
              <label className="block font-semibold mb-1">Name *</label>
              <input
                type="text"
                value={formName}
                onChange={(e) => setFormName(e.target.value)}
                maxLength={50}
                className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:text-white"
                placeholder="e.g., Work, Personal"
              />
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                {formName.length}/50 characters
              </p>
            </div>
            <div>
              <label className="block font-semibold mb-1">Color</label>
              <input
                type="color"
                value={formColor}
                onChange={(e) => setFormColor(e.target.value)}
                className="w-full h-10 px-1 py-1 border rounded-md cursor-pointer"
              />
            </div>
            <div>
              <label className="block font-semibold mb-1">Icon Name</label>
              <select
                value={formIcon}
                onChange={(e) => setFormIcon(e.target.value as IconName)}
                className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:text-white"
              >
                {iconOptions.map((option) => (
                  <option key={option.name} value={option.name}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          </div>
          {/* Preview */}
          <div className="mb-4">
            <label className="block font-semibold mb-2">Preview:</label>
            <span
              className="inline-block px-3 py-1 rounded-full text-sm font-medium"
              style={{
                backgroundColor: formColor,
                color: getContrastColor(formColor),
              }}
            >
              <CategoryIcon iconName={formIcon} className="inline mr-1" />
              {formName || "Category Name"}
            </span>
          </div>
          <div className="flex flex-col sm:flex-row gap-2">
            <button
              onClick={handleCreate}
              disabled={loading}
              className="px-4 py-2 bg-green-500 text-white rounded-md hover:bg-green-600 transition flex items-center justify-center gap-2 disabled:opacity-50"
            >
              <FaSave /> <span>Save</span>
            </button>
            <button
              onClick={cancelCreate}
              className="px-4 py-2 bg-gray-300 dark:bg-gray-700 rounded-md hover:bg-gray-400 dark:hover:bg-gray-600 transition flex items-center justify-center gap-2"
            >
              <FaTimes /> <span>Cancel</span>
            </button>
          </div>
        </div>
      )}

      {/* Categories Table */}
      <div className="overflow-x-auto border rounded-lg shadow -mx-4 sm:mx-0">
        <table className="w-full bg-white dark:bg-gray-800 min-w-[640px]">
          <thead className="bg-gray-200 dark:bg-gray-700">
            <tr>
              <th className="px-4 py-3 text-left">Name</th>
              <th className="px-4 py-3 text-left">Color</th>
              <th className="px-4 py-3 text-left">Icon</th>
              <th className="px-4 py-3 text-left">Preview</th>
              <th className="px-4 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {categories.length === 0 && !loading && (
              <tr>
                <td
                  colSpan={5}
                  className="px-4 py-6 text-center text-gray-500 italic"
                >
                  No categories yet. Create one to get started!
                </td>
              </tr>
            )}
            {categories.map((cat) => (
              <tr
                key={cat.id}
                className="border-t border-gray-200 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700"
              >
                {editingId === cat.id ? (
                  // Edit Mode
                  <>
                    <td className="px-4 py-3">
                      <input
                        type="text"
                        value={formName}
                        onChange={(e) => setFormName(e.target.value)}
                        maxLength={50}
                        className="w-full px-2 py-1 border rounded dark:bg-gray-700 dark:text-white"
                      />
                    </td>
                    <td className="px-4 py-3">
                      <input
                        type="color"
                        value={formColor}
                        onChange={(e) => setFormColor(e.target.value)}
                        className="w-20 h-8 px-1 border rounded cursor-pointer"
                      />
                    </td>
                    <td className="px-4 py-3">
                      <select
                        value={formIcon}
                        onChange={(e) =>
                          setFormIcon(e.target.value as IconName)
                        }
                        className="w-full px-2 py-1 border rounded dark:bg-gray-700 dark:text-white"
                      >
                        {iconOptions.map((option) => (
                          <option key={option.name} value={option.name}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className="inline-block px-3 py-1 rounded-full text-sm font-medium"
                        style={{
                          backgroundColor: formColor,
                          color: getContrastColor(formColor),
                        }}
                      >
                        <CategoryIcon
                          iconName={formIcon}
                          className="inline mr-1"
                        />
                        {formName}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={handleUpdate}
                        disabled={loading}
                        className="mr-2 text-green-600 hover:text-green-800 dark:text-green-400 disabled:opacity-50"
                        title="Save"
                      >
                        <FaSave />
                      </button>
                      <button
                        onClick={cancelEdit}
                        className="text-gray-600 hover:text-gray-800 dark:text-gray-400"
                        title="Cancel"
                      >
                        <FaTimes />
                      </button>
                    </td>
                  </>
                ) : (
                  // View Mode
                  <>
                    <td className="px-4 py-3 font-medium">{cat.name}</td>
                    <td className="px-4 py-3">
                      <div
                        className="w-12 h-6 rounded border"
                        style={{ backgroundColor: cat.color }}
                      ></div>
                    </td>
                    <td className="px-4 py-3 text-gray-600 dark:text-gray-400">
                      <CategoryIcon
                        iconName={cat.icon}
                        className="inline mr-2"
                      />
                      {iconOptions.find((o) => o.name === cat.icon)?.label ||
                        cat.icon}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className="inline-block px-3 py-1 rounded-full text-sm font-medium"
                        style={{
                          backgroundColor: cat.color,
                          color: getContrastColor(cat.color),
                        }}
                      >
                        <CategoryIcon
                          iconName={cat.icon}
                          className="inline mr-1"
                        />
                        {cat.name}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => startEdit(cat)}
                        className="mr-3 text-blue-600 hover:text-blue-800 dark:text-blue-400"
                        title="Edit"
                      >
                        <FaEdit />
                      </button>
                      <button
                        onClick={() => handleDelete(cat.id!)}
                        className="text-red-600 hover:text-red-800 dark:text-red-400"
                        title="Delete"
                      >
                        <FaTrash />
                      </button>
                    </td>
                  </>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Delete Confirmation Modal */}
      {categoryToDelete && (
        <DeleteModal
          message="Are you sure you want to delete this category? It will be removed from all documents."
          onConfirm={confirmDelete}
          onCancel={() => setCategoryToDelete(null)}
        />
      )}
    </div>
  );
}
