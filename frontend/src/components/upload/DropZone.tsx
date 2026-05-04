import { useRef, useState, useCallback } from 'react'
import { Upload } from 'lucide-react'
import { cn } from '@/lib/utils'

const MAX_SIZE_BYTES = 10 * 1024 * 1024

interface DropZoneProps {
  onFile: (file: File) => void
  disabled?: boolean
}

export function DropZone({ onFile, disabled }: DropZoneProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [isDragging, setIsDragging] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const validate = useCallback((file: File): string | null => {
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
      return 'Please upload a valid PDF file.'
    }
    if (file.size > MAX_SIZE_BYTES) {
      return 'File too large. Maximum size is 10MB.'
    }
    return null
  }, [])

  const handleFile = useCallback(
    (file: File) => {
      const err = validate(file)
      if (err) {
        setError(err)
        return
      }
      setError(null)
      onFile(file)
    },
    [validate, onFile],
  )

  const onDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      setIsDragging(false)
      const file = e.dataTransfer.files[0]
      if (file) handleFile(file)
    },
    [handleFile],
  )

  const onInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0]
      if (file) handleFile(file)
      e.target.value = ''
    },
    [handleFile],
  )

  const open = () => !disabled && inputRef.current?.click()

  return (
    <div className="flex flex-col items-center gap-3">
      <div
        role="button"
        tabIndex={disabled ? -1 : 0}
        onClick={open}
        onKeyDown={(e) => e.key === 'Enter' && open()}
        onDragOver={(e) => {
          e.preventDefault()
          setIsDragging(true)
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={onDrop}
        className={cn(
          'flex flex-col items-center justify-center gap-4 w-full rounded-xl border-2 border-dashed p-12 transition-all duration-200 cursor-pointer select-none',
          isDragging ? 'border-[#20EFA4] bg-[#20EFA4]/5' : 'border-zinc-700 hover:border-zinc-500',
          disabled && 'opacity-50 cursor-not-allowed',
        )}
      >
        <Upload size={40} style={{ color: '#20EFA4' }} strokeWidth={1.5} />
        <div className="text-center">
          <p className="text-zinc-300 text-sm font-medium">
            Drop your PDF here or{' '}
            <span className="underline" style={{ color: '#20EFA4' }}>
              click to browse
            </span>
          </p>
          <p className="mt-1 text-zinc-500 text-xs">PDF only · max 10MB</p>
        </div>
      </div>

      {error && <p className="text-red-400 text-sm">{error}</p>}

      <input
        ref={inputRef}
        type="file"
        accept="application/pdf,.pdf"
        className="hidden"
        onChange={onInputChange}
        disabled={disabled}
      />
    </div>
  )
}
