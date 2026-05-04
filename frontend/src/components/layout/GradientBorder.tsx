import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface GradientBorderProps {
  children: ReactNode
  className?: string
  rounded?: string
}

export function GradientBorder({
  children,
  className,
  rounded = 'rounded-2xl',
}: GradientBorderProps) {
  return (
    <div
      className={cn('p-px', rounded, className)}
      style={{ background: 'linear-gradient(135deg, #20EFA4 0%, #00FFFF 100%)' }}
    >
      <div className={cn('w-full h-full bg-zinc-950', rounded)}>{children}</div>
    </div>
  )
}
