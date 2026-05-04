import { useMutation } from '@tanstack/react-query'
import { queryDocument } from '@/services/api'

export const useDocumentQuery = (documentId: string) =>
  useMutation({
    mutationFn: (question: string) => queryDocument(documentId, question),
  })
