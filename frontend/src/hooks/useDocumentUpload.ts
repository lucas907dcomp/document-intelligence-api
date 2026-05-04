import { useMutation } from '@tanstack/react-query'
import { uploadDocument } from '@/services/api'

interface UploadOptions {
  onProgress?: (pct: number) => void
}

export const useDocumentUpload = ({ onProgress }: UploadOptions = {}) =>
  useMutation({
    mutationFn: (file: File) => uploadDocument(file, onProgress),
  })
