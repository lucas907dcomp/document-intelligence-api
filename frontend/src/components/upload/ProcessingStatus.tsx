import { motion } from 'framer-motion'
import { Loader2, CheckCircle2, XCircle } from 'lucide-react'
import { fadeIn } from '@/lib/animations'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { GradientBorder } from '@/components/layout/GradientBorder'
import type { DocumentStatus } from '@/types/api.types'

interface ProcessingStatusProps {
  status: DocumentStatus
  filename: string
  onRetry?: () => void
  errorMessage?: string
}

export function ProcessingStatus({
  status,
  filename,
  onRetry,
  errorMessage,
}: ProcessingStatusProps) {
  return (
    <motion.div variants={fadeIn} initial="hidden" animate="visible">
      <GradientBorder>
        <div
          className="rounded-2xl p-8 md:p-12 flex flex-col items-center gap-6"
          style={{
            background:
              'radial-gradient(ellipse 80% 50% at 50% 0%, rgba(32,239,164,0.08) 0%, transparent 70%), #09090b',
          }}
        >
          <div className="flex flex-col items-center gap-4 text-center">
            {status === 'PROCESSING' && (
              <>
                <Badge variant="processing">
                  <Loader2 size={12} className="animate-spin" />
                  Processing…
                </Badge>
                <p className="text-zinc-400 text-sm">
                  Analyzing{' '}
                  <span className="text-white font-medium">{filename}</span>
                </p>
                <p className="text-zinc-500 text-xs max-w-xs">
                  Extracting text, generating embeddings, building search index…
                </p>
              </>
            )}

            {status === 'READY' && (
              <>
                <Badge variant="ready">
                  <CheckCircle2 size={12} />
                  Ready
                </Badge>
                <p className="text-zinc-300 text-sm">
                  <span className="text-white font-medium">{filename}</span> is ready to
                  query.
                </p>
              </>
            )}

            {status === 'FAILED' && (
              <>
                <Badge variant="failed">
                  <XCircle size={12} />
                  Failed
                </Badge>
                <p className="text-zinc-400 text-sm max-w-xs">
                  {errorMessage ?? 'Processing failed. Please try with a different PDF.'}
                </p>
                {onRetry && (
                  <Button variant="ghost" onClick={onRetry} className="mt-2">
                    Try Again
                  </Button>
                )}
              </>
            )}
          </div>
        </div>
      </GradientBorder>
    </motion.div>
  )
}
