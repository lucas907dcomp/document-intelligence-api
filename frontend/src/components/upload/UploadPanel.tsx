import { useState, useCallback } from 'react'
import { motion } from 'framer-motion'
import { Loader2 } from 'lucide-react'
import { scaleIn } from '@/lib/animations'
import { GradientBorder } from '@/components/layout/GradientBorder'
import { Button } from '@/components/ui/button'
import { DropZone } from './DropZone'

interface UploadPanelProps {
  onUploadStart: (file: File) => void
  isUploading: boolean
  progress: number
}

export function UploadPanel({ onUploadStart, isUploading, progress }: UploadPanelProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null)

  const handleFile = useCallback((file: File) => setSelectedFile(file), [])

  const handleUpload = useCallback(() => {
    if (selectedFile) onUploadStart(selectedFile)
  }, [selectedFile, onUploadStart])

  return (
    <motion.div variants={scaleIn} initial="hidden" animate="visible">
      <GradientBorder>
        <div
          className="relative overflow-hidden rounded-2xl p-6 md:p-10"
          style={{
            background:
              'radial-gradient(ellipse 80% 50% at 50% 0%, rgba(32,239,164,0.10) 0%, transparent 70%), #09090b',
          }}
        >
          <DropZone onFile={handleFile} disabled={isUploading} />

          {selectedFile && !isUploading && (
            <div className="mt-6 flex flex-col items-center gap-3">
              <p className="text-zinc-400 text-sm">
                <span className="text-white font-medium">{selectedFile.name}</span>
                {' · '}
                {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
              </p>
              <Button onClick={handleUpload} className="rounded-full px-8">
                Upload &amp; Process
              </Button>
            </div>
          )}

          {isUploading && (
            <div className="mt-6 flex flex-col items-center gap-3">
              <p className="text-zinc-400 text-sm">
                Uploading{' '}
                <span className="text-white font-medium">{selectedFile?.name}</span>…
              </p>
              <div className="w-full max-w-sm h-1.5 bg-zinc-800 rounded-full overflow-hidden">
                <div
                  className="h-full rounded-full transition-all duration-300"
                  style={{
                    width: `${progress}%`,
                    background: 'linear-gradient(90deg, #20EFA4, #00FFFF)',
                  }}
                />
              </div>
              <div className="flex items-center gap-2 text-sm text-zinc-400">
                <Loader2 size={14} className="animate-spin" style={{ color: '#20EFA4' }} />
                {progress}%
              </div>
            </div>
          )}
        </div>
      </GradientBorder>
    </motion.div>
  )
}
