import { useRef, useEffect, useCallback, useState } from 'react'
import { motion } from 'framer-motion'
import { UploadCloud } from 'lucide-react'
import { scaleIn } from '@/lib/animations'
import { GradientBorder } from '@/components/layout/GradientBorder'
import { Button } from '@/components/ui/button'
import { MessageBubble, type Message } from './MessageBubble'
import { ChatInput } from './ChatInput'
import { useDocumentQuery } from '@/hooks/useDocumentQuery'

interface ChatInterfaceProps {
  documentId: string
  filename: string
  onReset: () => void
}

export function ChatInterface({ documentId, filename, onReset }: ChatInterfaceProps) {
  const [messages, setMessages] = useState<Message[]>([])
  const bottomRef = useRef<HTMLDivElement>(null)
  const { mutate: query, isPending } = useDocumentQuery(documentId)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = useCallback(
    (question: string) => {
      const userMsg: Message = {
        id: crypto.randomUUID(),
        role: 'user',
        content: question,
      }
      setMessages((prev) => [...prev, userMsg])

      query(question, {
        onSuccess: (res) => {
          const aiMsg: Message = {
            id: crypto.randomUUID(),
            role: 'assistant',
            content: res.data.answer,
            sourceChunks: res.data.sourceChunks,
          }
          setMessages((prev) => [...prev, aiMsg])
        },
        onError: () => {
          const errMsg: Message = {
            id: crypto.randomUUID(),
            role: 'assistant',
            content: 'Sorry, I encountered an error. Please try again.',
          }
          setMessages((prev) => [...prev, errMsg])
        },
      })
    },
    [query],
  )

  return (
    <motion.div variants={scaleIn} initial="hidden" animate="visible">
      <GradientBorder>
        <div
          className="rounded-2xl flex flex-col overflow-hidden"
          style={{
            background:
              'radial-gradient(ellipse 80% 40% at 50% 0%, rgba(32,239,164,0.08) 0%, transparent 70%), #09090b',
            minHeight: '520px',
            maxHeight: '72vh',
          }}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-zinc-800 flex-shrink-0">
            <div className="min-w-0">
              <p className="text-xs text-zinc-500 uppercase tracking-wider font-semibold">
                Active Document
              </p>
              <p className="text-white text-sm font-medium mt-0.5 truncate max-w-xs">
                {filename}
              </p>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={onReset}
              className="text-xs gap-1.5 flex-shrink-0 ml-4"
            >
              <UploadCloud size={13} />
              New Document
            </Button>
          </div>

          {/* Message list */}
          <div className="flex-1 overflow-y-auto p-6 flex flex-col gap-4 min-h-0">
            {messages.length === 0 && (
              <div className="flex flex-col items-center justify-center flex-1 text-center py-16">
                <p className="text-zinc-500 text-sm">
                  Document ready — ask me anything about it.
                </p>
              </div>
            )}
            {messages.map((msg) => (
              <MessageBubble key={msg.id} message={msg} />
            ))}
            <div ref={bottomRef} />
          </div>

          {/* Input */}
          <div className="px-6 py-4 border-t border-zinc-800 flex-shrink-0">
            <ChatInput onSend={handleSend} isLoading={isPending} />
          </div>
        </div>
      </GradientBorder>
    </motion.div>
  )
}
