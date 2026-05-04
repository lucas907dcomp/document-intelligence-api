import { useState, useCallback } from 'react'
import { Send, Loader2 } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'

interface ChatInputProps {
  onSend: (question: string) => void
  isLoading: boolean
  disabled?: boolean
}

export function ChatInput({ onSend, isLoading, disabled }: ChatInputProps) {
  const [value, setValue] = useState('')

  const handleSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault()
      const q = value.trim()
      if (!q || isLoading) return
      onSend(q)
      setValue('')
    },
    [value, isLoading, onSend],
  )

  return (
    <form onSubmit={handleSubmit} className="flex gap-3">
      <Input
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="Ask anything about your document…"
        disabled={disabled || isLoading}
        className="flex-1"
      />
      <Button
        type="submit"
        disabled={!value.trim() || isLoading || disabled}
        className="rounded-full flex-shrink-0 px-5"
      >
        {isLoading ? (
          <Loader2 size={15} className="animate-spin" />
        ) : (
          <Send size={15} />
        )}
        <span className="hidden sm:inline">{isLoading ? 'Thinking…' : 'Ask'}</span>
      </Button>
    </form>
  )
}
