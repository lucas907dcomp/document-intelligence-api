export interface UploadResponse {
  documentId: string
}

export type DocumentStatus = 'PROCESSING' | 'READY' | 'FAILED'

export interface StatusResponse {
  documentId: string
  status: DocumentStatus
  filename: string
}

export interface QueryRequest {
  question: string
}

export interface QueryResponse {
  answer: string
  sourceChunks: string[]
}

export interface ApiError {
  type: string
  title: string
  status: number
  detail: string
  instance?: string
}
