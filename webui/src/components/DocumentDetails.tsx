import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  getDocumentById,
  getOcrStatus,
  updateDocument,
  deleteDocument,
} from "../lib/documentService";
import { formatFileSize, tryCatch } from "../lib/utils";
import type { DocumentDetailDto } from "../lib/types";
import {
  FaHouse,
  FaPencil,
  FaSpinner,
  FaCheck,
  FaTrash,
} from "react-icons/fa6";
import { FaTimes } from "react-icons/fa";
import { DeleteModal } from "./DeleteModal";

const LOCALE = "de-AT";

export default function DocumentDetails() {
  const { id } = useParams<{ id: string }>();
  const [document, setDocument] = useState<DocumentDetailDto | null>(null);
  const [ocrStatus, setOcrStatus] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [editedTitle, setEditedTitle] = useState("");
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const navigate = useNavigate();

  // Load document once
  useEffect(() => {
    if (!id) return;
    setLoading(true);
    tryCatch(getDocumentById(id)).then(([doc, err]) => {
      setLoading(false);
      if (err) console.error(err);
      else if (doc) {
        setDocument(doc);
        setOcrStatus(doc.ocrStatus);
        setEditedTitle(doc.title);
      }
    });
  }, [id]);

  // Poll status if OCR is pending/processing
  useEffect(() => {
    if (!id || !ocrStatus) return;
    if (ocrStatus === "COMPLETED" || ocrStatus === "FAILED") return;

    const pollInterval = setInterval(async () => {
      const [status, err] = await tryCatch(getOcrStatus(id));
      if (err) {
        console.error("Failed to fetch OCR status:", err);
        return;
      }

      if (status) {
        setOcrStatus(status.ocrStatus);

        // If completed, reload full document to get OCR text
        if (status.ocrStatus === "COMPLETED" || status.ocrStatus === "FAILED") {
          const [doc, docErr] = await tryCatch(getDocumentById(id));
          if (!docErr && doc) {
            setDocument(doc);
          }
        }
      }
    }, 3000); // Poll every 3 seconds

    return () => clearInterval(pollInterval);
  }, [id, ocrStatus]);

  const handleStartEdit = () => {
    setIsEditingTitle(true);
    setEditedTitle(document?.title || "");
  };

  const handleCancelEdit = () => {
    setIsEditingTitle(false);
    setEditedTitle(document?.title || "");
  };

  const handleSaveTitle = async () => {
    if (!document || !editedTitle.trim()) return;

    const [updated, err] = await tryCatch(
      updateDocument(document.id, { ...document, title: editedTitle.trim() }),
    );

    if (err) {
      console.error("Failed to update title:", err);
      alert("Failed to update title");
      return;
    }

    if (updated) {
      setDocument({ ...document, title: updated.title });
      setIsEditingTitle(false);
    }
  };

  const handleDelete = async () => {
    if (!document) return;

    const [, err] = await tryCatch(deleteDocument(document.id));

    if (err) {
      console.error("Failed to delete document:", err);
      alert("Failed to delete document");
      return;
    }

    // Navigate to home after successful deletion
    navigate("/");
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      handleSaveTitle();
    } else if (e.key === "Escape") {
      e.preventDefault();
      handleCancelEdit();
    }
  };

  if (loading)
    return <p className="text-gray-500 mt-6 text-center">Loading...</p>;
  if (!document)
    return <p className="text-red-500 mt-6 text-center">Document not found.</p>;

  return (
    <div className="max-w-4xl mx-auto mt-6 p-6 border rounded-lg shadow-md bg-white dark:bg-gray-800 flex flex-col gap-6">
      {/* Header */}
      <div className="flex justify-between items-start">
        <div className="flex-1">
          {isEditingTitle ? (
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={editedTitle}
                onChange={(e) => setEditedTitle(e.target.value)}
                onKeyDown={handleKeyDown}
                className="text-3xl font-bold px-2 py-1 border border-blue-500 rounded-md bg-white dark:bg-gray-900 dark:text-white flex-1"
                autoFocus
              />
              <button
                onClick={handleSaveTitle}
                className="p-2 bg-green-500 text-white rounded-md hover:bg-green-600 transition"
                title="Save (Enter)"
              >
                <FaCheck />
              </button>
              <button
                onClick={handleCancelEdit}
                className="p-2 bg-red-500 text-white rounded-md hover:bg-red-600 transition"
                title="Cancel (Esc)"
              >
                <FaTimes />
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <h1 className="text-3xl font-bold">{document.title}</h1>
              <button
                onClick={handleStartEdit}
                className="p-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition"
                title="Edit Title"
              >
                <FaPencil />
              </button>
              <button
                onClick={() => setShowDeleteModal(true)}
                className="p-2 bg-red-500 text-white rounded-md hover:bg-red-600 transition"
                title="Delete Document"
              >
                <FaTrash />
              </button>
            </div>
          )}
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-2">
            {document.originalFilename}
          </p>
        </div>

        <div className="flex flex-col items-end gap-2">
          <div className="text-sm text-gray-500 dark:text-gray-400 text-right">
            <div>
              <span className="font-semibold">Created:</span>{" "}
              {new Date(document.createdAt).toLocaleString(LOCALE)}
            </div>
            {document.ocrProcessedAt && (
              <div>
                <span className="font-semibold">OCR Processed:</span>{" "}
                {new Date(document.ocrProcessedAt).toLocaleString(LOCALE)}
              </div>
            )}
          </div>
          <button
            onClick={() => navigate("/")}
            className="p-2 bg-gray-300 dark:bg-gray-700 rounded-md hover:bg-gray-400 dark:hover:bg-gray-600 transition"
            title="Home"
          >
            <FaHouse />
          </button>
        </div>
      </div>

      {/* OCR Status */}
      <div className="p-4 rounded-lg border border-gray-200 dark:border-gray-700">
        <div className="flex items-center gap-2 mb-2">
          <span className="font-semibold">OCR Status:</span>
          {ocrStatus === "PENDING" && (
            <span className="flex items-center gap-2 text-yellow-600 dark:text-yellow-400">
              <FaSpinner className="animate-spin" />
              Pending
            </span>
          )}
          {ocrStatus === "PROCESSING" && (
            <span className="flex items-center gap-2 text-blue-600 dark:text-blue-400">
              <FaSpinner className="animate-spin" />
              Processing
            </span>
          )}
          {ocrStatus === "COMPLETED" && (
            <span className="text-green-600 dark:text-green-400">
              ✓ Completed
            </span>
          )}
          {ocrStatus === "FAILED" && (
            <span className="text-red-600 dark:text-red-400">✗ Failed</span>
          )}
        </div>

        {document.ocrError && (
          <p className="text-sm text-red-500 mt-2">
            Error: {document.ocrError}
          </p>
        )}
      </div>

      {/* OCR Text */}
      {ocrStatus === "COMPLETED" && document.ocrText && (
        <div className="p-4 rounded-lg border border-gray-200 dark:border-gray-700">
          <h2 className="text-xl font-semibold mb-3">Extracted Text</h2>
          <div className="bg-gray-50 dark:bg-gray-900 p-4 rounded-md max-h-96 overflow-y-auto">
            <pre className="whitespace-pre-wrap text-sm font-mono">
              {document.ocrText}
            </pre>
          </div>
        </div>
      )}

      {/* Download Button */}
      {document.downloadUrl && (
        <a
          href={document.downloadUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="px-6 py-3 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition text-center"
        >
          Download Original PDF
        </a>
      )}

      {/* Document Metadata */}
      <div className="flex justify-between items-center text-sm">
        <div>
          <span className="font-semibold">File Size:</span>{" "}
          {formatFileSize(document.fileSize)}
        </div>
        <div className="text-right">
          <span className="font-semibold">Content Type:</span>{" "}
          {document.contentType}
        </div>
      </div>

      {/* Delete Modal */}
      {showDeleteModal && (
        <DeleteModal
          onConfirm={handleDelete}
          onCancel={() => setShowDeleteModal(false)}
        />
      )}
    </div>
  );
}
