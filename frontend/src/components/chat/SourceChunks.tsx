import { useState } from 'react'
import { ChevronDown, FileText } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'

interface SourceChunksProps {
  chunks: string[]
}

export function SourceChunks({ chunks }: SourceChunksProps) {
  const [open, setOpen] = useState(false)

  return (
    <div className="w-full">
      <button
        onClick={() => setOpen((v) => !v)}
        className="flex items-center gap-1.5 text-xs text-zinc-500 hover:text-zinc-400 transition-colors"
      >
        <FileText size={11} style={{ color: '#20EFA4' }} />
        <span>
          {chunks.length} source {chunks.length === 1 ? 'passage' : 'passages'}
        </span>
        <ChevronDown
          size={11}
          className={`transition-transform duration-200 ${open ? 'rotate-180' : ''}`}
        />
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            key="chunks"
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <div className="mt-2 flex flex-col gap-2">
              {chunks.map((chunk, i) => (
                <div
                  key={i}
                  className="rounded-lg border border-zinc-800 bg-zinc-950 p-3"
                >
                  <p className="text-xs font-mono text-zinc-400 leading-relaxed line-clamp-4">
                    {chunk}
                  </p>
                  <p className="mt-1.5 text-xs text-zinc-600">Passage {i + 1}</p>
                </div>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
