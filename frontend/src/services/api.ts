import axios from 'axios'
import type { UploadResponse, StatusResponse, QueryResponse } from '@/types/api.types'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 30_000,
})

export const uploadDocument = (
  file: File,
  onProgress?: (pct: number) => void,
) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post<UploadResponse>('/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) =>
      onProgress?.(Math.round((e.loaded * 100) / (e.total ?? 1))),
  })
}

export const getDocumentStatus = (id: string) =>
  api.get<StatusResponse>(`/documents/${id}/status`)

export const queryDocument = (id: string, question: string) =>
  api.post<QueryResponse>(`/documents/${id}/query`, { question })
