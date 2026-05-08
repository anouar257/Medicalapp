/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',
  content: ['./src/**/*.{html,ts}'],
  safelist: [
    'focus-visible:ring-fuchsia-500/80',
    'bg-violet-500/30',
    'ring-violet-400/50',
    'ring-violet-400/60',
    'bg-white/5',
    'ring-white/10',
    'hover:bg-white/10',
    'hover:bg-white/70',
  ],
  theme: {
    extend: {},
  },
  plugins: [],
};
