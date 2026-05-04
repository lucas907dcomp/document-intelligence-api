import { motion } from 'framer-motion'
import { Bot, User } from 'lucide-react'
import { fadeInUp } from '@/lib/animations'
import { SourceChunks } from './SourceChunks'

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  sourceChunks?: string[]
}

interface MessageBubbleProps {
  message: Message
}

export function MessageBubble({ message }: MessageBubbleProps) {
  const isUser = message.role === 'user'

  return (
    <motion.div
      variants={fadeInUp}
      initial="hidden"
      animate="visible"
      className={`flex gap-3 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}
    >
      <div
        className={`flex-shrink-0 h-8 w-8 rounded-full flex items-center justify-center ${
          isUser ? 'bg-zinc-700' : 'bg-zinc-900 border border-zinc-700'
        }`}
      >
        {isUser ? (
          <User size={14} className="text-zinc-300" />
        ) : (
          <Bot size={14} style={{ color: '#20EFA4' }} />
        )}
      </div>

      <div
        className={`flex flex-col gap-2 max-w-[80%] ${isUser ? 'items-end' : 'items-start'}`}
      >
        <div
          className={`rounded-2xl px-4 py-3 text-sm leading-relaxed ${
            isUser
              ? 'bg-zinc-800 text-white rounded-tr-sm'
              : 'border border-zinc-700 bg-zinc-900/50 text-zinc-200 rounded-tl-sm'
          }`}
        >
          {message.content}
        </div>

        {!isUser && message.sourceChunks && message.sourceChunks.length > 0 && (
          <SourceChunks chunks={message.sourceChunks} />
        )}
      </div>
    </motion.div>
  )
}
