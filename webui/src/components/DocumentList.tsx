import type { DocumentSummaryDto } from "../lib/types";
import { DocumentCard } from "./DocumentCard";

type Props = {
  documents: DocumentSummaryDto[];
  onDelete: (id: string) => void;
};

export function DocumentList({ documents, onDelete }: Props) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      {documents.map((doc) => (
        <DocumentCard key={doc.id} document={doc} onDelete={onDelete} />
      ))}
    </div>
  );
}
