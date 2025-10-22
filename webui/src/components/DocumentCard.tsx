import { Link } from "react-router-dom";
import { FaTrash, FaPencil } from "react-icons/fa6";
import type { DocumentSummaryDto } from "../lib/types";

type Props = {
  document: DocumentSummaryDto;
  onDelete: (id: string) => void;
};

export function DocumentCard({ document, onDelete }: Props) {
  return (
    <div className="p-4 border border-gray-300 dark:border-gray-600 rounded-lg shadow-sm transform transition-transform hover:scale-105 hover:bg-gray-100 dark:hover:bg-gray-800">
      <div className="flex justify-between items-center gap-1">
        <Link
          to={`/documents/${document.id}`}
          className="text-lg font-semibold flex-1 hover:underline"
        >
          {document.title}
        </Link>

        <div className="flex gap-2">
          <Link
            to={`/documents/${document.id}/edit`}
            className="text-gray-600 dark:text-gray-300 hover:text-blue-500 transition"
          >
            <FaPencil />
          </Link>

          <button
            onClick={() => onDelete(document.id)}
            className="text-gray-600 dark:text-gray-300 hover:text-red-500 transition"
          >
            <FaTrash />
          </button>
        </div>
      </div>
    </div>
  );
}
