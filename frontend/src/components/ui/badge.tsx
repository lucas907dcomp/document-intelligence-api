import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const badgeVariants = cva(
  'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors',
  {
    variants: {
      variant: {
        default: 'border-zinc-700 bg-zinc-900 text-zinc-300',
        processing: 'border-yellow-900/50 bg-yellow-950/30 text-yellow-400',
        ready: 'border-[#20EFA4]/30 bg-[#20EFA4]/10 text-[#20EFA4]',
        failed: 'border-red-900/50 bg-red-950/30 text-red-400',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  },
)

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return <div className={cn(badgeVariants({ variant }), className)} {...props} />
}

export { Badge, badgeVariants }
