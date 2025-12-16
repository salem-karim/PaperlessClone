export type DocumentDto = {
  title: string;
  file: File;
  categories?: CategoryDto[];
};

export type CategoryDto = {
  id?: string;
  name: string;
  color: string;
  icon: string;
  createdAt?: string;
  updatedAt?: string;
};

// Used in dashboard lists
export type DocumentSummaryDto = {
  id: string;
  title: string;
  fileSize: number;
  originalFilename: string;
  contentType: string;
  processingStatus: ProcessingStatus;
  createdAt: string;
  categories: CategoryDto[];
};

// Used on the detail page
export type DocumentDetailDto = DocumentSummaryDto & {
  downloadUrl: string;
  ocrText?: string;
  ocrTextDownloadUrl?: string;
  summaryText?: string;
  processingError?: string;
  ocrProcessedAt?: string;
  genaiProcessedAt?: string;
};

type SuccessResult<T> = readonly [T, null];

type ErrorResult<E = Error> = readonly [null, E];

export type Result<T, E = Error> = SuccessResult<T> | ErrorResult<E>;

export type ProcessingStatus =
  | "PENDING"
  | "OCR_PROCESSING"
  | "OCR_COMPLETED"
  | "GENAI_PROCESSING"
  | "COMPLETED"
  | "OCR_FAILED"
  | "GENAI_FAILED";
