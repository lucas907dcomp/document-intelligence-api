import { motion } from 'framer-motion'
import { fadeInUp, staggerContainer } from '@/lib/animations'

export function HeroSection() {
  return (
    <motion.section
      className="flex flex-col items-center text-center px-6 py-20 md:py-28"
      variants={staggerContainer}
      initial="hidden"
      animate="visible"
    >
      <motion.p
        variants={fadeInUp}
        className="text-xs font-semibold tracking-widest text-zinc-500 uppercase mb-6"
      >
        Document Intelligence · RAG Pipeline
      </motion.p>

      <motion.h1
        variants={fadeInUp}
        className="text-5xl md:text-7xl font-bold tracking-tight text-white leading-tight max-w-4xl"
      >
        Chat with your{' '}
        <span
          className="bg-clip-text text-transparent"
          style={{ backgroundImage: 'linear-gradient(90deg, #20EFA4, #00FFFF)' }}
        >
          PDF documents
        </span>
      </motion.h1>

      <motion.p
        variants={fadeInUp}
        className="mt-6 text-lg text-zinc-300 max-w-lg leading-relaxed"
      >
        Upload any PDF and ask questions in natural language. Answers come with the
        exact source passages from your document.
      </motion.p>
    </motion.section>
  )
}
