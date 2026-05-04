import type { Config } from 'tailwindcss'

export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        'accent-mint': '#20EFA4',
        'accent-cyan': '#00FFFF',
      },
      backgroundImage: {
        'gradient-accent': 'linear-gradient(135deg, #20EFA4 0%, #00FFFF 100%)',
        'glow-accent':
          'radial-gradient(ellipse 80% 50% at 50% 0%, rgba(32,239,164,0.12), transparent)',
      },
      animation: {
        'spin-slow': 'spin 3s linear infinite',
      },
    },
  },
  plugins: [],
} satisfies Config
