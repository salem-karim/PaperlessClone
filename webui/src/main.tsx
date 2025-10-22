import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.tsx";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import DocumentForm from "./components/DocumentForm.tsx";
import DocumentDetails from "./components/DocumentDetails.tsx";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter>
      {/* <Navbar /> will show on all pages*/}
      <Routes>
        <Route path="/" element={<App />} />
        <Route path="/documents/new" element={<DocumentForm />} />
        <Route path="/documents/:id" element={<DocumentDetails />} />
      </Routes>
    </BrowserRouter>
  </StrictMode>,
);
