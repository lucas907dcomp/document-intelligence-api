import { useQuery } from '@tanstack/react-query'
import { getDocumentStatus } from '@/services/api'
import type { StatusResponse } from '@/types/api.types'
import type { AxiosResponse } from 'axios'

export const useDocumentStatus = (documentId: string | null) =>
  useQuery<AxiosResponse<StatusResponse>, Error, StatusResponse>({
    queryKey: ['document-status', documentId],
    queryFn: () => getDocumentStatus(documentId!),
    enabled: !!documentId,
    refetchInterval: (query) => {
      const status = (query.state.data as AxiosResponse<StatusResponse> | undefined)
        ?.data.status
      return status === 'PROCESSING' ? 2000 : false
    },
    select: (res) => res.data,
  })
