export type DocumentDto = {
  title: string;
  file: File;
};

// Used in dashboard lists
export type DocumentSummaryDto = {
  id: string;
  title: string;
  fileSize: number;
  originalFilename: string;
  contentType: string;
  ocrStatus: OcrStatus;
  createdAt: string;
};

// Used on the detail page
export type DocumentDetailDto = DocumentSummaryDto & {
  downloadUrl: string;
  ocrText?: string;
  ocrTextDownloadUrl?: string;
  ocrError?: string;
  ocrProcessedAt?: string;
};

type SuccessResult<T> = readonly [T, null];

type ErrorResult<E = Error> = readonly [null, E];

export type Result<T, E = Error> = SuccessResult<T> | ErrorResult<E>;

export type OcrStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
