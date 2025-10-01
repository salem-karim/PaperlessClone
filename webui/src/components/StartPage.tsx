import { useEffect, useState } from "react";
import { getDocuments } from "../lib/documentService";
import { tryCatch } from "../lib/try-catch";
import type { DocumentDto } from "../lib/types";

export default function StartPage() {
  const [documents, setDocuments] = useState<DocumentDto[]>([]);
  const [loading, setLoading] = useState(false);

  async function loadDocuments() {
    setLoading(true);
    const [docs, docsError] = await tryCatch<DocumentDto[]>(getDocuments());
    setLoading(false);

    if (docsError) {
      console.error(docsError.message);
    } else if (docs) {
      setDocuments(docs);
    }
  }

  useEffect(() => {
    loadDocuments();
  }, []);

  return (
    <>
      <header
        style={{ display: "flex", gap: "1rem", alignItems: "center" }}
      ></header>

      <h1>Documents</h1>

      {loading && <p>Loading documents...</p>}

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
          gap: "1rem",
        }}
      >
        {documents.map((doc) => (
          <div
            key={doc.id}
            style={{
              padding: "1rem",
              border: "1px solid #ccc",
              borderRadius: "8px",
              boxShadow: "0 2px 6px rgba(0,0,0,0.1)",
              cursor: "pointer",
              transition: "transform 0.2s",
            }}
            onClick={() => alert(`Clicked document: ${doc.title}`)}
            onMouseEnter={(e) =>
              (e.currentTarget.style.transform = "scale(1.03)")
            }
            onMouseLeave={(e) => (e.currentTarget.style.transform = "scale(1)")}
          >
            <h2 style={{ fontSize: "1.1rem", margin: 0 }}>{doc.title}</h2>
          </div>
        ))}
      </div>
    </>
  );
}
