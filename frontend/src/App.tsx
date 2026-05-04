import { useState, useCallback, useEffect } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { GeometricBackground } from '@/components/layout/GeometricBackground'
import { HeroSection } from '@/components/hero/HeroSection'
import { UploadPanel } from '@/components/upload/UploadPanel'
import { ProcessingStatus } from '@/components/upload/ProcessingStatus'
import { ChatInterface } from '@/components/chat/ChatInterface'
import { useDocumentUpload } from '@/hooks/useDocumentUpload'
import { useDocumentStatus } from '@/hooks/useDocumentStatus'
import type { AxiosError } from 'axios'
import type { ApiError } from '@/types/api.types'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, refetchOnWindowFocus: false },
  },
})

type AppPhase = 'idle' | 'uploading' | 'processing' | 'ready' | 'failed'

interface AppState {
  phase: AppPhase
  documentId: string | null
  filename: string
  progress: number
  errorMessage: string
}

const INITIAL_STATE: AppState = {
  phase: 'idle',
  documentId: null,
  filename: '',
  progress: 0,
  errorMessage: '',
}

function resolveUploadError(err: unknown): string {
  const axiosErr = err as AxiosError<ApiError>
  const status = axiosErr?.response?.status
  if (status === 413) return 'File too large. Maximum size is 10MB.'
  if (status === 400) return 'Invalid file. Please upload a PDF.'
  if (status === 503) return 'Service temporarily unavailable. Try again later.'
  return 'Upload failed. Please try again.'
}

function AppContent() {
  const [state, setState] = useState<AppState>(INITIAL_STATE)

  const progressRef = useCallback(
    (pct: number) => setState((s) => ({ ...s, progress: pct })),
    [],
  )

  const { mutate: upload } = useDocumentUpload({ onProgress: progressRef })

  const { data: statusData } = useDocumentStatus(
    state.phase === 'processing' ? state.documentId : null,
  )

  useEffect(() => {
    if (state.phase !== 'processing' || !statusData) return
    if (statusData.status === 'READY') {
      setState((s) => (s.phase === 'processing' ? { ...s, phase: 'ready' } : s))
    } else if (statusData.status === 'FAILED') {
      setState((s) =>
        s.phase === 'processing'
          ? {
              ...s,
              phase: 'failed',
              errorMessage: 'Document processing failed. Please try with a different PDF.',
            }
          : s,
      )
    }
  }, [statusData, state.phase])

  const handleUploadStart = useCallback(
    (file: File) => {
      setState((s) => ({ ...s, phase: 'uploading', filename: file.name, progress: 0 }))
      upload(file, {
        onSuccess: (res) => {
          setState((s) => ({
            ...s,
            phase: 'processing',
            documentId: res.data.documentId,
          }))
        },
        onError: (err) => {
          setState((s) => ({
            ...s,
            phase: 'failed',
            errorMessage: resolveUploadError(err),
          }))
        },
      })
    },
    [upload],
  )

  const handleReset = useCallback(() => {
    queryClient.clear()
    setState(INITIAL_STATE)
  }, [])

  return (
    <div className="relative min-h-screen bg-black text-white">
      <GeometricBackground />

      <div className="relative z-10 max-w-3xl mx-auto px-4">
        <HeroSection />

        <div className="pb-20">
          {(state.phase === 'idle' || state.phase === 'uploading') && (
            <UploadPanel
              onUploadStart={handleUploadStart}
              isUploading={state.phase === 'uploading'}
              progress={state.progress}
            />
          )}

          {state.phase === 'processing' && (
            <ProcessingStatus status="PROCESSING" filename={state.filename} />
          )}

          {state.phase === 'ready' && state.documentId && (
            <ChatInterface
              documentId={state.documentId}
              filename={state.filename}
              onReset={handleReset}
            />
          )}

          {state.phase === 'failed' && (
            <ProcessingStatus
              status="FAILED"
              filename={state.filename}
              errorMessage={state.errorMessage}
              onRetry={handleReset}
            />
          )}
        </div>
      </div>
    </div>
  )
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppContent />
    </QueryClientProvider>
  )
}
