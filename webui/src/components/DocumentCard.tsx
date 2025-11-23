import { Link, useLocation } from "react-router-dom";
import { FaTrash } from "react-icons/fa6";
import type { DocumentSummaryDto } from "../lib/types";
import { formatDate, formatFileSize } from "../lib/utils";

type Props = {
  document: DocumentSummaryDto;
  onDelete: (id: string) => void;
};

export function DocumentCard({ document, onDelete }: Props) {
  const location = useLocation();

  return (
    <div className="p-4 border border-gray-300 dark:border-gray-600 rounded-lg shadow-sm transform transition-transform hover:scale-105 hover:bg-gray-100 dark:hover:bg-gray-800">
      <div className="flex justify-between items-center gap-1 mb-2">
        <Link
          to={`/documents/${document.id}`}
          state={{ from: location.pathname + location.search }}
          className="text-lg font-semibold flex-1 hover:underline"
        >
          {document.title}
        </Link>

        <button
          onClick={() => onDelete(document.id)}
          className="text-gray-600 dark:text-gray-300 hover:text-red-500 transition cursor-pointer"
        >
          <FaTrash />
        </button>
      </div>

      <div className="text-sm text-gray-500 dark:text-gray-400 flex justify-between">
        <span>{formatFileSize(document.fileSize)}</span>
        <span>{formatDate(document.createdAt)}</span>
      </div>
    </div>
  );
}
