import { useEffect, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import {
  getDocumentById,
  getProcessingStatus,
  updateDocument,
  deleteDocument,
  downloadDocument,
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
  const navigate = useNavigate();
  const location = useLocation();
  const [document, setDocument] = useState<DocumentDetailDto | null>(null);
  const [processingStatus, setProcessingStatus] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [editedTitle, setEditedTitle] = useState("");
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  // Get the previous search query from location state
  const previousSearch = (location.state as { from?: string })?.from || "/";

  // Load document once
  useEffect(() => {
    if (!id) return;
    setLoading(true);
    tryCatch(getDocumentById(id)).then(([doc, err]) => {
      setLoading(false);
      if (err) console.error(err);
      else if (doc) {
        setDocument(doc);
        setProcessingStatus(doc.processingStatus);
        setEditedTitle(doc.title);
      }
    });
  }, [id]);

  // Poll status if document is still processing
  useEffect(() => {
    if (!id || !processingStatus) return;
    if (processingStatus === "COMPLETED" || processingStatus === "OCR_FAILED" || processingStatus === "GENAI_FAILED") return;

    const pollInterval = setInterval(async () => {
      const [status, err] = await tryCatch(getProcessingStatus(id));
      if (err) {
        console.error("Failed to fetch processing status:", err);
        return;
      }

      if (status) {
        setProcessingStatus(status.processingStatus);

        // If completed or failed, reload full document to get results
        if (status.processingStatus === "COMPLETED" || 
            status.processingStatus === "OCR_FAILED" || 
            status.processingStatus === "GENAI_FAILED") {
          const [doc, docErr] = await tryCatch(getDocumentById(id));
          if (!docErr && doc) {
            setDocument(doc);
          }
        }
      }
    }, 3000); // Poll every 3 seconds

    return () => clearInterval(pollInterval);
  }, [id, processingStatus]);

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

    // Navigate back to previous search or home
    navigate(previousSearch);
  };

  const handleDownload = async () => {
    if (!document) return;

    try {
      await downloadDocument(document.id, document.originalFilename);
    } catch (err) {
      console.error("Failed to download document:", err);
      alert("Failed to download document");
    }
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
          <button
            onClick={() => navigate(previousSearch)} // ← Use previousSearch instead of "/"
            className="p-2 bg-gray-300 dark:bg-gray-700 rounded-md hover:bg-gray-400 dark:hover:bg-gray-600 transition"
            title="Home"
          >
            <FaHouse />
          </button>
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
        </div>
      </div>

      {/* Processing Status */}
      <div className="p-4 rounded-lg border border-gray-200 dark:border-gray-700">
        <div className="flex items-center gap-2 mb-2">
          <span className="font-semibold">Processing Status:</span>
          {processingStatus === "PENDING" && (
            <span className="flex items-center gap-2 text-yellow-600 dark:text-yellow-400">
              <FaSpinner className="animate-spin" />
              Waiting to start...
            </span>
          )}
          {processingStatus === "OCR_PROCESSING" && (
            <span className="flex items-center gap-2 text-blue-600 dark:text-blue-400">
              <FaSpinner className="animate-spin" />
              Extracting text (OCR)...
            </span>
          )}
          {processingStatus === "OCR_COMPLETED" && (
            <span className="flex items-center gap-2 text-blue-600 dark:text-blue-400">
              <FaSpinner className="animate-spin" />
              OCR complete, waiting for summarization...
            </span>
          )}
          {processingStatus === "GENAI_PROCESSING" && (
            <span className="flex items-center gap-2 text-blue-600 dark:text-blue-400">
              <FaSpinner className="animate-spin" />
              Generating AI summary...
            </span>
          )}
          {processingStatus === "COMPLETED" && (
            <span className="text-green-600 dark:text-green-400">
              ✓ Processing Complete
            </span>
          )}
          {processingStatus === "OCR_FAILED" && (
            <span className="text-red-600 dark:text-red-400">✗ OCR Failed</span>
          )}
          {processingStatus === "GENAI_FAILED" && (
            <span className="text-orange-600 dark:text-orange-400">⚠ AI Summarization Failed (OCR succeeded)</span>
          )}
        </div>

        {document.processingError && (
          <p className="text-sm text-red-500 mt-2">
            Error: {document.processingError}
          </p>
        )}
      </div>

      {/* OCR Text - Only shown when GenAI fails as fallback */}
      {processingStatus === "GENAI_FAILED" && document.ocrText && (
        <div className="p-4 rounded-lg border border-orange-200 dark:border-orange-700 bg-orange-50 dark:bg-orange-900/20">
          <h2 className="text-xl font-semibold mb-3">Extracted Text (Fallback)</h2>
          <p className="text-sm text-gray-600 dark:text-gray-400 mb-3">
            AI summarization failed. Showing raw OCR text instead.
          </p>
          <div className="bg-white dark:bg-gray-900 p-4 rounded-md max-h-96 overflow-y-auto">
            <pre className="whitespace-pre-wrap text-sm font-mono">
              {document.ocrText}
            </pre>
          </div>
        </div>
      )}

      {/* AI Summary - Rendered as Markdown */}
      {processingStatus === "COMPLETED" && document.summaryText && (
        <div className="p-4 rounded-lg border border-blue-200 dark:border-blue-700 bg-blue-50 dark:bg-blue-900/20">
          <h2 className="text-xl font-semibold mb-3">AI Summary</h2>
          <div className="bg-white dark:bg-gray-900 p-4 rounded-md prose prose-sm dark:prose-invert max-w-none">
            <ReactMarkdown
              components={{
                // Customize code blocks
                code: ({ className, children, ...props }) => {
                  const match = /language-(\w+)/.exec(className || '');
                  return match ? (
                    <pre className="bg-gray-100 dark:bg-gray-800 p-3 rounded-md overflow-x-auto">
                      <code className={className} {...props}>
                        {children}
                      </code>
                    </pre>
                  ) : (
                    <code
                      className="px-1.5 py-0.5 bg-gray-100 dark:bg-gray-800 rounded text-sm font-mono"
                      {...props}
                    >
                      {children}
                    </code>
                  );
                },
                // Style headings
                h1: ({ children }) => (
                  <h1 className="text-2xl font-bold mt-4 mb-2">{children}</h1>
                ),
                h2: ({ children }) => (
                  <h2 className="text-xl font-bold mt-3 mb-2">{children}</h2>
                ),
                h3: ({ children }) => (
                  <h3 className="text-lg font-semibold mt-2 mb-1">{children}</h3>
                ),
                // Style lists
                ul: ({ children }) => (
                  <ul className="list-disc list-inside ml-4 mb-2">{children}</ul>
                ),
                ol: ({ children }) => (
                  <ol className="list-decimal list-inside ml-4 mb-2">{children}</ol>
                ),
                // Style links
                a: ({ children, href }) => (
                  <a
                    href={href}
                    className="text-blue-600 dark:text-blue-400 hover:underline"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {children}
                  </a>
                ),
              }}
            >
              {document.summaryText}
            </ReactMarkdown>
          </div>
        </div>
      )}

      {/* Download Button */}
      <button
        onClick={handleDownload}
        className="px-6 py-3 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition text-center w-full"
      >
        Download Original File
      </button>

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
